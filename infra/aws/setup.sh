#!/usr/bin/env bash
# One-time AWS setup for the demo, as it was done for this account. Idempotent: safe to run again.
#
#   AWS_PROFILE=cuy ./setup.sh                  # uses your current public IP for the admin access
#   AWS_PROFILE=cuy ADMIN_CIDR=1.2.3.4/32 ./setup.sh
#
# What it does
#   1. IAM role for the EC2: Amazon Bedrock (invoke models), SSM, and read ONE secret (the DB password)
#   2. Lets containers reach the instance role (metadata hop limit 2) so the AI service needs no AWS key
#   3. Makes the RDS publicly reachable and opens port 5432 ONLY to ADMIN_CIDR (the EC2 already has access)
# To close the database again:  ./close-db.sh
set -euo pipefail
export AWS_PAGER="" AWS_DEFAULT_REGION="${AWS_DEFAULT_REGION:-us-east-1}"

INSTANCE_ID="${INSTANCE_ID:-i-06a1efa93cf5cd3bd}"
DB_ID="${DB_ID:-cuy-monitor-db}"
ROLE=cuy-ec2-role
PROFILE=cuy-ec2-profile
ADMIN_CIDR="${ADMIN_CIDR:-$(curl -s https://checkip.amazonaws.com | tr -d '\n')/32}"

here="$(cd "$(dirname "$0")" && pwd)"
secret_arn="$(aws rds describe-db-instances --db-instance-identifier "$DB_ID" \
  --query 'DBInstances[0].MasterUserSecret.SecretArn' --output text)"
db_sg="$(aws rds describe-db-instances --db-instance-identifier "$DB_ID" \
  --query 'DBInstances[0].VpcSecurityGroups[0].VpcSecurityGroupId' --output text)"

echo "== 1. instance role"
if ! aws iam get-role --role-name "$ROLE" >/dev/null 2>&1; then
  aws iam create-role --role-name "$ROLE" --description "cuy-monitor EC2: Bedrock, SSM and the DB secret" \
    --assume-role-policy-document '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"ec2.amazonaws.com"},"Action":"sts:AssumeRole"}]}' >/dev/null
fi
aws iam attach-role-policy --role-name "$ROLE" --policy-arn arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore
aws iam put-role-policy --role-name "$ROLE" --policy-name cuy-bedrock-and-secret --policy-document "{
  \"Version\":\"2012-10-17\",\"Statement\":[
    {\"Sid\":\"BedrockInvoke\",\"Effect\":\"Allow\",\"Action\":[\"bedrock:InvokeModel\",\"bedrock:InvokeModelWithResponseStream\",\"bedrock:GetInferenceProfile\",\"bedrock:ListInferenceProfiles\"],\"Resource\":\"*\"},
    {\"Sid\":\"ReadDbSecret\",\"Effect\":\"Allow\",\"Action\":[\"secretsmanager:GetSecretValue\"],\"Resource\":\"$secret_arn\"}]}"
aws iam get-instance-profile --instance-profile-name "$PROFILE" >/dev/null 2>&1 || \
  aws iam create-instance-profile --instance-profile-name "$PROFILE" >/dev/null
aws iam add-role-to-instance-profile --instance-profile-name "$PROFILE" --role-name "$ROLE" 2>/dev/null || true
if [ "$(aws ec2 describe-iam-instance-profile-associations --filters Name=instance-id,Values="$INSTANCE_ID" --query 'length(IamInstanceProfileAssociations)' --output text)" = "0" ]; then
  sleep 10
  aws ec2 associate-iam-instance-profile --instance-id "$INSTANCE_ID" --iam-instance-profile Name="$PROFILE" >/dev/null
fi

echo "== 2. containers can use the instance role"
aws ec2 modify-instance-metadata-options --instance-id "$INSTANCE_ID" --http-tokens required \
  --http-put-response-hop-limit 2 --http-endpoint enabled >/dev/null

echo "== 3. public database, port 5432 only for $ADMIN_CIDR"
aws ec2 authorize-security-group-ingress --group-id "$db_sg" \
  --ip-permissions "IpProtocol=tcp,FromPort=5432,ToPort=5432,IpRanges=[{CidrIp=$ADMIN_CIDR,Description=demo admin}]" 2>/dev/null || echo "(rule already there)"
aws rds modify-db-instance --db-instance-identifier "$DB_ID" --publicly-accessible --apply-immediately >/dev/null
echo "Done. Endpoint: $(aws rds describe-db-instances --db-instance-identifier "$DB_ID" --query 'DBInstances[0].Endpoint.Address' --output text)"

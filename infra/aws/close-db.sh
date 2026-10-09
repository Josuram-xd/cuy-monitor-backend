#!/usr/bin/env bash
# Closes the demo database again: no public access and no admin rule in its security group.
#   AWS_PROFILE=cuy ./close-db.sh
set -euo pipefail
export AWS_PAGER="" AWS_DEFAULT_REGION="${AWS_DEFAULT_REGION:-us-east-1}"
DB_ID="${DB_ID:-cuy-monitor-db}"

db_sg="$(aws rds describe-db-instances --db-instance-identifier "$DB_ID" \
  --query 'DBInstances[0].VpcSecurityGroups[0].VpcSecurityGroupId' --output text)"
aws rds modify-db-instance --db-instance-identifier "$DB_ID" --no-publicly-accessible --apply-immediately >/dev/null

# remove every CIDR rule on 5432 (the rule that lets the EC2 in is a security-group reference and stays)
for cidr in $(aws ec2 describe-security-groups --group-ids "$db_sg" \
    --query 'SecurityGroups[0].IpPermissions[?FromPort==`5432`].IpRanges[].CidrIp' --output text); do
  aws ec2 revoke-security-group-ingress --group-id "$db_sg" --protocol tcp --port 5432 --cidr "$cidr" >/dev/null
  echo "Closed 5432 for $cidr"
done
echo "The database is private again."

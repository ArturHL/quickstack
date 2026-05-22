#!/bin/bash
set -e

# Configuración
REGION="mx-central-1"
BUCKET_NAME="quickstack-terraform-state-mx"
TABLE_NAME="quickstack-terraform-locks"

echo "==== Bootstrap de Estado Remoto Terraform ===="
echo "Región: $REGION"
echo "Bucket S3: $BUCKET_NAME"
echo "DynamoDB: $TABLE_NAME"
echo "----------------------------------------------"

# 1. Crear Bucket S3
echo "[1/4] Comprobando bucket S3..."
if aws s3 ls "s3://$BUCKET_NAME" 2>&1 | grep -q 'NoSuchBucket'; then
    echo "Creando bucket S3 '$BUCKET_NAME'..."
    aws s3api create-bucket \
        --bucket $BUCKET_NAME \
        --region $REGION \
        --create-bucket-configuration LocationConstraint=$REGION
else
    echo "El bucket ya existe."
fi

# 2. Habilitar versionado
echo "[2/4] Habilitando versionado en el bucket..."
aws s3api put-bucket-versioning \
    --bucket $BUCKET_NAME \
    --versioning-configuration Status=Enabled

# 3. Bloquear acceso público
echo "[3/4] Bloqueando acceso público al bucket..."
aws s3api put-public-access-block \
    --bucket $BUCKET_NAME \
    --public-access-block-configuration "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"

# 4. Crear tabla DynamoDB
echo "[4/4] Comprobando tabla DynamoDB..."
if aws dynamodb describe-table --table-name $TABLE_NAME --region $REGION 2>&1 | grep -q 'ResourceNotFoundException'; then
    echo "Creando tabla DynamoDB '$TABLE_NAME'..."
    aws dynamodb create-table \
        --table-name $TABLE_NAME \
        --attribute-definitions AttributeName=LockID,AttributeType=S \
        --key-schema AttributeName=LockID,KeyType=HASH \
        --billing-mode PAY_PER_REQUEST \
        --region $REGION
else
    echo "La tabla DynamoDB ya existe."
fi

echo "=============================================="
echo "¡Bootstrap completado con éxito!"
echo "El entorno local está listo para 'terraform init'."

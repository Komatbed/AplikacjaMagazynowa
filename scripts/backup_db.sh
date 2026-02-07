#!/bin/bash

# Configuration
CONTAINER_NAME="warehouse-backup"
BACKUP_DIR="/backups"
DATE=$(date +%Y%m%d_%H%M%S)
FILENAME="backup_$DATE.sql"

# Log function
log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1"
}

log "Starting database backup..."

# Check if container is running
if ! docker ps | grep -q $CONTAINER_NAME; then
    log "Error: Container $CONTAINER_NAME is not running."
    exit 1
fi

# Run backup
# We use sh -c to execute the entire command line inside the container
docker exec $CONTAINER_NAME sh -c "PGPASSWORD=\$POSTGRES_PASSWORD pg_dump -h db -U postgres warehouse > $BACKUP_DIR/$FILENAME"

if [ $? -eq 0 ]; then
    log "Backup created successfully: $FILENAME"
else
    log "Error: Backup failed"
    exit 1
fi

# Cleanup old backups (older than 7 days)
log "Cleaning up old backups..."
docker exec $CONTAINER_NAME sh -c "find $BACKUP_DIR -name 'backup_*.sql' -mtime +7 -delete"

log "Backup process completed."

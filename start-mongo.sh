#!/usr/bin/env bash

echo "=== Avvio Ambiente di Sviluppo MongoDB ==="
container system start
# 1. Assicuriamoci che la cartella dei dati esista (altrimenti il bind mount fallisce)
mkdir -p mongo_data

# 2. Controlliamo se il container è già stato creato in passato
if container list -a | grep -q "DesiderioDb"; then
    echo "Container 'DesiderioDb' trovato. Sto avviando..."
    container start DesiderioDb
else
    echo "Container 'DesiderioDb' non trovato. Lo creo per la prima volta..."
    container run --name DesiderioDb \
      -p 27017:27017 \
      -v "$(pwd)/mongo_data:/data/db" \
      --entrypoint mongod \
      -d mongo:latest --bind_ip_all
fi

echo "=== Database pronto! ==="



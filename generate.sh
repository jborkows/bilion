#!/bin/bash
docker build -t bilion_data . -f DockerFileData
docker run -v ./data:/out bilion_data
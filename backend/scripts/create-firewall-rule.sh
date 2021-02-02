#/bin/sh
gcloud compute firewall-rules create default-allow-http-8081 \
	--allow tcp:8081 \
	--source-ranges 0.0.0.0/0 \
	--target-tags http-server \
	--description "Allow port 8080 access to http-server"

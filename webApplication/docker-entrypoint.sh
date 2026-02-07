#!/bin/sh

# Default to localhost if not set
API_URL=${API_URL:-http://localhost:8080/api/v1}

# Create env-config.js
echo "window.env = {" > /usr/share/nginx/html/js/env-config.js
echo "  API_URL: \"$API_URL\"" >> /usr/share/nginx/html/js/env-config.js
echo "};" >> /usr/share/nginx/html/js/env-config.js

# Execute the CMD (nginx)
exec "$@"

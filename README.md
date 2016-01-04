# redirect-server
A minimal http server that issues HTTP redirects (response code 302, `Location` header)
to whitelisted hosts.

Two basic modes are supported:

1. If `redirectPrefix` is unspecified, the client is redirected directly to the location
specified in the `redirect` query parameter.

2. If `redirectPrefix` is specified, the client is redirected to a URL constructed by 
appending the URL-encoded representation of the location requested in the `redirect`
query parameter to the value specified for `redirectPrefix`.

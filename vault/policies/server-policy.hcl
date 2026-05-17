path "pki/sign/server-role" {
  capabilities = ["update"]
}

path "pki/ca/pem" {
  capabilities = ["read"]
}

path "auth/token/renew-self" {
  capabilities = ["update"]
}

path "auth/token/lookup-self" {
  capabilities = ["read"]
}

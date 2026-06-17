# User API Spec

## Register User

Endpoint : POST /api/auth/login
Request Body :

```json
{
  "username": "ihsan",
  "password": "rahasia",
  "name": "Ihsan Santana Wibawa"
}
```

Response Body (Success) :

```json
{
  "data": "OK"
}
```

Response Body (Error) :

```json
{
  "errors": "Username must not blank, ???"
}
```

## Login User

Endpoint : POST /api/auth/login
Request Body :

```json
{
  "username": "ihsan",
  "password": "rahasia"
}
```

Response Body (Success) :

```json
{
  "data": {
    "token": "TOKEN",
    "expiredAt": 123456543 // milisecond
  }
}
```

Response Body (Error) :

```json
{
  "errors": "Username and Password Salah"
}
```

## Get User

Endpoint : GET /api/users/current

Request Header :

- X-API-TOKEN = Token (Mandatory, comes from login response)

Response Body (Success) :

```json
{
  "data": {
    "username": "ihsan",
    "name": "Ihsan Santana Wibawa"
  }
}
```

Response Body (Error) :

```json
{
  "errors": "Unauthorized"
}
```

## Update User

Endpoint : PATCH /api/users/current
Request Header :

- X-API-TOKEN = Token (Mandatory, comes from login response)
  Request Body :

```json
{
  "name": "Ihsan Santana Wibawa", // put if only want to update name
  "password": "rahasia" // put if only want to update password
}
```

Response Body (Success) :

```json
{
  "data": {
    "username": "ihsan",
    "name": "Ihsan Santana Wibawa"
  }
}
```

Response Body (Error) :

```json
{
  "errors": "Unauthorized"
}
```

## Logout

Endpoint : DELETE /api/auth/logout

Request Header :

- X-API-TOKEN = Token (Mandatory, comes from login response)

Response Body (Success) :

```json
{
  "data": "OK"
}
```

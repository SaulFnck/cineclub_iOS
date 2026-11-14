# Despliegue de CineClub API en Render

## 1. Arquitectura del despliegue

```
GitHub  ->  Render (Docker)  ->  Tomcat 10.1 + WAR  ->  TiDB Cloud (MySQL)
```

- La app es un **WAR** (Jersey 3.1.5 / Jakarta EE 10), así que Render la ejecuta
  con el `Dockerfile` de la raíz: compila con Maven y la publica en Tomcat 10.1.
- El WAR se llama `cineclub_api.war`, por eso el contexto es `/cineclub_api`.
- Base de datos: **TiDB Cloud** (`gateway01.us-west-2.prod.aws.tidbcloud.com:4000`).

URLs resultantes (cambia `TU-SERVICIO` por tu subdominio de Render):

| Recurso | URL |
|---|---|
| Raíz | `https://TU-SERVICIO.onrender.com/cineclub_api/` |
| Health check | `https://TU-SERVICIO.onrender.com/cineclub_api/resources/test-db` |
| Login | `https://TU-SERVICIO.onrender.com/cineclub_api/resources/auth/login` |
| Películas | `https://TU-SERVICIO.onrender.com/cineclub_api/resources/peliculas` |

---

## 2. Variables de entorno en Render

En **Render Dashboard -> tu servicio -> Environment** agrega:

| Variable | Valor | Secreto |
|---|---|---|
| `DB_URL` | `jdbc:mysql://gateway01.us-west-2.prod.aws.tidbcloud.com:4000/cineclub?sslMode=VERIFY_IDENTITY&enabledTLSProtocols=TLSv1.2,TLSv1.3` | No |
| `DB_USER` | `2EjGEaBT1g5hz1A.root` | No |
| `DB_PASSWORD` | tu contraseña de TiDB | **Sí** |
| `PORT` | `8080` | No |

> Las variables distinguen mayúsculas/minúsculas y no deben tener espacios
> alrededor del `=`.

`ConexionBD` lee esas variables con `System.getenv`. Si no existen, usa los
valores por defecto del código (pensados para desarrollo local).

---

## 3. Pasos para desplegar

1. **Sube el proyecto a GitHub.** La raíz del repositorio debe ser la carpeta
   `CINECLUB_API` (la que contiene `Dockerfile`, `render.yaml` y la carpeta
   `cineclub_api/`). Si subes solo `cineclub_api/`, el `COPY cineclub_api/...`
   del Dockerfile fallará.
2. En Render: **New -> Blueprint** y selecciona el repositorio.
   - Render detecta `render.yaml` y crea el servicio `cineclub-api`.
   - Alternativa manual: **New -> Web Service -> Docker**, con
     `Dockerfile Path = ./Dockerfile` y `Docker Context = .`.
3. Cuando Render pida los valores de `DB_URL`, `DB_USER` y `DB_PASSWORD`,
   complétalos (o agrégalos después en **Environment**).
4. Espera el build. El health check debe responder 200 en `/cineclub_api/`.
5. Verifica la base de datos con:

   ```bash
   curl https://TU-SERVICIO.onrender.com/cineclub_api/resources/test-db
   ```

   Debe devolver `{"estado":"OK", ...}` con los conteos de las tablas.

---

## 4. Permisos de red en TiDB Cloud

Render usa IPs de salida dinámicas. En **TiDB Cloud -> tu clúster ->
Network Access / IP Allowlist** debes permitir el tráfico público, por ejemplo
`0.0.0.0/0` (o el rango que TiDB indique para acceso público). Si no, verás
errores de conexión aunque las variables estén correctas.

---

## 5. Probar en local

El código sigue funcionando sin variables de entorno (usa los valores por
defecto). Si quieres probar el mismo flujo que Render, define las variables
antes de arrancar Tomcat:

```powershell
$env:DB_URL="jdbc:mysql://gateway01.us-west-2.prod.aws.tidbcloud.com:4000/cineclub?sslMode=VERIFY_IDENTITY&enabledTLSProtocols=TLSv1.2,TLSv1.3"
$env:DB_USER="2EjGEaBT1g5hz1A.root"
$env:DB_PASSWORD="tu_password"
```

En NetBeans, el servidor Tomcat hereda las variables del sistema si lo inicias
desde una terminal, o puedes agregarlas en la configuración de arranque.

Para usar Bruno contra producción, selecciona el entorno **Render** y cambia
`TU-SERVICIO` por tu subdominio en
`bruno/CineClub/environments/Render.bru`.

---

## 6. Notas y solución de problemas

- **Plan gratuito**: el servicio se suspende tras ~15 min de inactividad; la
  primera petición puede tardar 30–60 s.
- **`No open ports detected`**: asegúrate de que `PORT=8080` esté definido en
  Render (Tomcat escucha en 8080, no en 10000).
- **404 en `/resources/...`**: falta el contexto. Usa
  `/cineclub_api/resources/...`.
- **Error de conexión TLS**: no cambies `sslMode=VERIFY_IDENTITY`; revisa la
  allowlist de TiDB.
- **Zona horaria**: Render ejecuta en UTC. Las fechas de rentas se guardan y
  calculan en UTC, lo que mantiene la consistencia interna, pero pueden verse
  desplazadas respecto a la hora de México. Si necesitas hora local, configura
  `-Duser.timezone=America/Mexico_City` en `JAVA_OPTS` y ajusta también la
  sesión de MySQL/TiDB.
- **Seguridad**: la contraseña de TiDB todavía existe como valor por defecto en
  `ConexionBD.java`. Cuando confirmes que Render funciona, **rota la contraseña
  en TiDB**, actualiza `DB_PASSWORD` en Render y elimina los valores por defecto
  del código para no publicar credenciales en GitHub.

-- ===============================================
-- CONFIGURACIÓN PARA MODO REMOTE
-- Ejecutar estos comandos en SQL*Plus como usuario MASTER
-- para crear vistas que luego serán visibles en modo REMOTE
-- ===============================================

-- ===============================================
-- 1. CREAR VISTAS QUE SÍ FUNCIONARÁN
-- ===============================================

-- Vista de tablas del usuario actual
CREATE OR REPLACE VIEW VISTA_MIS_TABLAS AS
SELECT 
    table_name,
    num_rows,
    blocks,
    last_analyzed
FROM user_tables
ORDER BY table_name;

-- Vista de objetos del usuario
CREATE OR REPLACE VIEW VISTA_MIS_OBJETOS AS
SELECT 
    object_name,
    object_type,
    status,
    created
FROM user_objects
WHERE object_type IN ('TABLE', 'VIEW', 'INDEX')
ORDER BY created DESC;

-- Vista de información de sesión
CREATE OR REPLACE VIEW VISTA_SESION_INFO AS
SELECT 
    'Usuario' as tipo,
    USER as valor,
    'Actual' as estado
FROM dual
UNION ALL
SELECT 
    'Fecha',
    TO_CHAR(SYSDATE, 'DD/MM/YYYY HH24:MI:SS'),
    'Sistema'
FROM dual
UNION ALL
SELECT
    'Base de Datos',
    (SELECT name FROM v$database),
    'Conectada'
FROM dual;

-- Vista de estadísticas de tablespace
CREATE OR REPLACE VIEW VISTA_TABLESPACE AS
SELECT 
    tablespace_name,
    status,
    contents,
    extent_management
FROM user_tablespaces;

-- ===============================================
-- 2. CONFIGURAR DATABASE LINKS (Angel <-> Bell)
-- ===============================================

-- En Angel (para conectar a Bell):
CREATE DATABASE LINK LINK_TO_BELL
CONNECT TO C##isaProyect IDENTIFIED BY isaProyect
USING '(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=Bell)(PORT=1521))(CONNECT_DATA=(SERVICE_NAME=orcl)))';

-- En Bell (para conectar a Angel):
CREATE DATABASE LINK LINK_TO_ANGEL
CONNECT TO pastazProyect IDENTIFIED BY pastazProyect
USING '(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=Angel)(PORT=1521))(CONNECT_DATA=(SERVICE_NAME=orcl)))';

-- ===============================================
-- 3. CREAR VISTAS REMOTAS VIA DATABASE LINK
-- ===============================================

-- En Angel, crear vista que accede datos de Bell (usando dual para prueba):
CREATE OR REPLACE VIEW VISTA_DATOS_BELL AS
SELECT 
    'Bell' as origen,
    'Conectado' as estado,
    SYSDATE as fecha_consulta,
    USER as usuario_remoto
FROM dual@LINK_TO_BELL;

-- En Bell, crear vista que accede datos de Angel (usando dual para prueba):
CREATE OR REPLACE VIEW VISTA_DATOS_ANGEL AS
SELECT 
    'Angel' as origen,
    'Conectado' as estado,
    SYSDATE as fecha_consulta,
    USER as usuario_remoto
FROM dual@LINK_TO_ANGEL;

-- ===============================================
-- 4. VERIFICAR CONFIGURACIÓN
-- ===============================================

-- Verificar vistas creadas
SELECT view_name, text_length, read_only 
FROM user_views 
ORDER BY view_name;

-- Verificar privilegios otorgados
SELECT table_name, grantee, privilege, grantable
FROM user_tab_privs_made
WHERE table_name LIKE 'VISTA_%'
ORDER BY table_name;

-- Verificar database links
SELECT db_link, username, host, created
FROM user_db_links;

-- Probar conectividad de database link
SELECT COUNT(*) FROM dual@LINK_TO_BELL;

-- ===============================================
-- 5. COMANDOS DE PRUEBA PARA MODO REMOTE
-- ===============================================

-- Estos comandos funcionarán en modo REMOTE de la aplicación:

-- Ver vistas disponibles (será detectado automáticamente)
SELECT view_name FROM user_views;

-- Ver vistas con privilegios de otros usuarios
SELECT owner||'.'||table_name as vista_completa, privilege
FROM user_tab_privs 
WHERE table_name IN (SELECT view_name FROM all_views WHERE owner = user_tab_privs.owner);

-- Contenido de vistas (solo lectura) - ESTAS VISTAS SÍ EXISTEN:
SELECT * FROM VISTA_MIS_TABLAS WHERE ROWNUM <= 10;
SELECT * FROM VISTA_MIS_OBJETOS WHERE ROWNUM <= 10;
SELECT * FROM VISTA_SESION_INFO;
SELECT * FROM VISTA_TABLESPACE WHERE ROWNUM <= 5;

-- Vistas remotas via database link (solo si el link funciona)
SELECT * FROM VISTA_DATOS_BELL WHERE ROWNUM <= 10;

-- ===============================================
-- NOTAS IMPORTANTES:
-- ===============================================

/*
1. MODO MASTER:
   - Ve y gestiona tablas propias (USER_TABLES)
   - Puede hacer INSERT, UPDATE, DELETE, SELECT
   - Ve tablas con privilegios de otros usuarios
   - Gestión completa de datos

2. MODO REMOTE:
   - Solo ve vistas (USER_VIEWS)
   - Solo modo lectura (SELECT únicamente)
   - Ve vistas creadas por usuarios master
   - Ve vistas remotas via database links
   - NO puede modificar datos

3. FLEXIBILIDAD:
   - Cualquier computadora puede ser Master o Remote
   - El modo se elige en el menú, no por ubicación
   - Misma aplicación funciona en Angel y Bell
   - Connection strings dinámicos según elección
*/

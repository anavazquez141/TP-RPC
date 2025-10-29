from flask import Flask
import atexit

# Importar Blueprints
from auth_controller import auth_bp
from usuario_controller import usuario_bp
from evento_controller import evento_bp
from donacion_controller import donacion_bp
from oferta_controller import oferta_bp
from solicitud_controller import solicitud_bp
from participacion_controller import participacion_bp
# Importar clientes gRPC
from cliente_usuario import ClienteUsuario
from cliente_evento import ClienteEvento
from cliente_donacion import ClienteDonacion

# Inicializar Flask
app = Flask(
    __name__,
    template_folder="views/templates",
    static_folder="views/static"
)

from datetime import datetime

# Filtro para formatear fechas
@app.template_filter('datetimeformat')
def datetimeformat(value, format='%d/%m/%Y %H:%M'):
    if isinstance(value, datetime):
        return value.strftime(format)
    return value  # Si no es datetime, devolver tal cual

app.secret_key = "super_secret_key"

# Instanciar clientes gRPC
cliente_usuario = ClienteUsuario()
cliente_evento = ClienteEvento()
cliente_donacion = ClienteDonacion()

# Registrar Blueprints
app.register_blueprint(auth_bp)
app.register_blueprint(usuario_bp)
app.register_blueprint(evento_bp)
app.register_blueprint(donacion_bp)
app.register_blueprint(oferta_bp)
app.register_blueprint(solicitud_bp)
app.register_blueprint(participacion_bp, url_prefix='/participacion')


# Funciones de cierre para clientes gRPC
atexit.register(cliente_usuario.cerrar)
atexit.register(cliente_evento.cerrar)
atexit.register(cliente_donacion.cerrar)

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5001)

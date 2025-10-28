from flask import Blueprint, render_template, request, session, redirect, url_for, flash
from utils import requiere_autenticacion, requiere_rol_presidente_o_coordinador
from cliente_evento_graph import ClienteEventoGraphQL

participacion_bp = Blueprint('participacion_bp', __name__, template_folder='templates')

@participacion_bp.route('/informe_participacion', methods=['GET', 'POST'])
@requiere_autenticacion
def informe_participacion():
    token = session.get('token')
    if not token:
        flash("Debes iniciar sesión primero", "error")
        return redirect(url_for('auth_bp.login'))

    rol = session.get('rol', '').upper()
    es_admin = rol in ['PRESIDENTE', 'COORDINADOR']
    usuario_actual = session.get('username')

    cliente = ClienteEventoGraphQL()

    # Cargar usuarios solo para admins
    usuarios = []
    if es_admin:
        try:
            usuarios = cliente.obtener_usuarios(token)
        except Exception as e:
            flash(f"Error al cargar usuarios: {e}", "error")

    # Filtros
    fecha_desde = request.args.get('fechaDesde')
    fecha_hasta = request.args.get('fechaHasta')
    usuario_filtro = request.args.get('usuario')

    # Forzar usuario propio si no es admin
    if not es_admin:
        usuario_filtro = usuario_actual

    informe = []
    if usuario_filtro:
        try:
            informe = cliente.informe_participacion(
                token=token,
                usuario=usuario_filtro,
                fecha_desde=fecha_desde,
                fecha_hasta=fecha_hasta
            )
            if not informe:
                flash("No se encontraron participaciones con los filtros aplicados", "info")
        except Exception as e:
            flash(f"Error al obtener el informe: {e}", "error")

    return render_template(
        'informe_participacion.html',
        informe=informe,
        usuarios=usuarios,
        es_admin=es_admin,
        usuario_actual=usuario_actual,
        request=request
    )
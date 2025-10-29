from flask import Blueprint, render_template, request, session, redirect, url_for, flash
from utils import requiere_autenticacion
from cliente_usuario import ClienteUsuario
from cliente_evento_graph import ClienteEventoGraphQL

cliente_usuario = ClienteUsuario()
participacion_bp = Blueprint('participacion_bp', __name__)

@participacion_bp.route('/informe_participacion', methods=['GET', 'POST'])
@requiere_autenticacion(cliente_usuario)
def informe_participacion():
    token = session.get('token')
    email = session.get('email')

    if not token or not email:
        flash("Debes iniciar sesión primero", "error")
        return redirect(url_for('auth_bp.login'))

    # --- USUARIO ACTUAL ---
    try:
        usuario = cliente_usuario.traer_usuario_por_email(email, token)
        if not usuario:
            flash("Usuario no encontrado", "error")
            return redirect(url_for('auth.index'))
    except Exception as e:
        flash(f"Error al obtener usuario: {e}", "error")
        return redirect(url_for('auth.index'))

    session['username'] = usuario.nombreUsuario
    session['rol'] = usuario.rol

    es_admin = usuario.rol in [0, 1]  # Presidente o Coordinador
    es_voluntario = usuario.rol == 2
    nombre_usuario_actual = usuario.nombreUsuario

    # --- FILTROS ---
    fecha_desde = request.args.get('fechaDesde')
    fecha_hasta = request.args.get('fechaHasta')
    usuario_filtro = request.args.get('usuario')

    # --- FORZAR USUARIO ---
    if es_voluntario:
        usuario_filtro = nombre_usuario_actual
    elif not usuario_filtro:
        flash("El usuario es obligatorio", "error")
        usuario_filtro = nombre_usuario_actual  # Default para admin

    # --- GRAPHQL ---
    cliente = ClienteEventoGraphQL()
    informe = []
    try:
        informe = cliente.informe_participacion(
            token=token,
            usuario=usuario_filtro,
            fecha_desde=fecha_desde,
            fecha_hasta=fecha_hasta
        )
        if not informe:
            flash("No se encontraron participaciones", "info")
    except Exception as e:
        flash(f"Error al obtener informe: {e}", "error")

    return render_template(
        'informe_participacion.html',
        informe=informe,
        es_admin=es_admin,
        es_voluntario=es_voluntario,
        usuario_actual=nombre_usuario_actual,
        usuario_filtro=usuario_filtro,  # ← para mantener el valor
        request=request
    )
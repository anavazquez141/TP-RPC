from flask import Blueprint, render_template, request, redirect, url_for, session, flash
from utils import requiere_autenticacion, mapear_rol
from cliente_usuario import ClienteUsuario

auth_bp = Blueprint('auth', __name__)
cliente_usuario = ClienteUsuario()

@auth_bp.route('/')
@requiere_autenticacion(cliente_usuario)
def index():
    usuario = cliente_usuario.traer_usuario_por_email(session['email'], session['token'])
    if usuario is None:
        session.clear()
        flash("Error al obtener datos del usuario", "error")
        return redirect(url_for('auth.login'))

    usuario_dict = {
        "id": usuario.id,
        "nombreUsuario": usuario.nombreUsuario,
        "nombre": usuario.nombre,
        "apellido": usuario.apellido,
        "telefono": usuario.telefono,
        "email": usuario.email,
        "rol": mapear_rol(usuario.rol),
        "activo": usuario.activo
    }
    es_presidente = usuario.rol == 0
    return render_template('perfil.html', usuario=usuario_dict, es_presidente=es_presidente)

@auth_bp.route('/login', methods=['GET', 'POST'])
def login():
    if 'token' in session:
        try:
            response = cliente_usuario.validar_token(session['token'])
            if response and response.status == "SUCCESS":
                flash("Ya estás logueado", "info")
                return redirect(url_for('auth.index'))
        except Exception:
            session.clear()

    if request.method == 'POST':
        email = request.form['email']
        clave = request.form['clave']
        try:
            response = cliente_usuario.login(email, clave)
            if response is None:
                flash("Error en el servidor", "error")
                return render_template('login.html')
            if response.status == "SUCCESS":
                session['token'] = response.token
                session['email'] = email
                flash("Inicio de sesión exitoso", "success")
                return redirect(url_for('auth.index'))
            else:
                flash(response.message, "error")
        except Exception as e:
            flash(f"Error al iniciar sesión: {e}", "error")
    return render_template('login.html')

@auth_bp.route('/logout')
def logout():
    if 'token' in session:
        try:
            response = cliente_usuario.logout(session['token'])
            if response and response.status == "SUCCESS":
                flash("Cierre de sesión exitoso", "success")
            else:
                flash("Error al cerrar sesión", "error")
        except Exception as e:
            flash(f"Error al cerrar sesión: {e}", "error")
    session.clear()
    return redirect(url_for('auth.login'))

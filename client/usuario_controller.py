from flask import Blueprint, render_template, request, redirect, url_for, flash, session
from utils import requiere_autenticacion, requiere_rol_presidente, mapear_rol
from cliente_usuario import ClienteUsuario

usuario_bp = Blueprint('usuarios', __name__)
cliente_usuario = ClienteUsuario()

@usuario_bp.route('/')
@requiere_autenticacion(cliente_usuario)
@requiere_rol_presidente(cliente_usuario)
def listar_usuarios():
    try:
        response = cliente_usuario.listar_usuarios(session['token'])
        usuarios = response.usuarios if response else []
    except Exception:
        usuarios = []
        flash("Error al obtener la lista de usuarios", "error")
    return render_template('usuarios.html', usuarios=usuarios)

@usuario_bp.route('/registrar', methods=['GET', 'POST'])
@requiere_autenticacion(cliente_usuario)
@requiere_rol_presidente(cliente_usuario)
def registrar_usuario():
    if request.method == 'POST':
        try:
            response = cliente_usuario.registrar_usuario(
                request.form['nombreUsuario'],
                request.form['nombre'],
                request.form['apellido'],
                request.form['telefono'],
                request.form['email'],
                request.form['rol'],
                session['token']
            )
            if response.status == "SUCCESS":
                flash("Usuario registrado exitosamente", "success")
                return redirect(url_for('usuarios.listar_usuarios'))
            else:
                flash(response.message, "error")
        except Exception as e:
            flash(f"Error: {str(e)}", "error")
    return render_template('registrar.html')


@usuario_bp.route('/cuenta', methods=['GET', 'POST'])
@requiere_autenticacion(cliente_usuario)
def cuenta():
    usuario = cliente_usuario.traer_usuario_por_email(session['email'], session['token'])
    if usuario is None:
        session.clear()
        flash("Error al obtener datos del usuario", "error")
        return redirect(url_for('login'))
    
    if request.method == 'POST':
        if 'eliminar' in request.form:
            try:
                response = cliente_usuario.eliminar_usuario(usuario.id, session['token'])
                if response and response.success:
                    flash("Usuario dado de baja exitosamente", "success")
                    session.clear()
                    return redirect(url_for('login'))
                else:
                    flash(response.message if response else "Error al eliminar cuenta", "error")
            except Exception as e:
                print(f"Error al eliminar cuenta: {e}")
                flash("Error al eliminar cuenta", "error")
        else:
            nombre_usuario = request.form['nombreUsuario']
            nombre = request.form['nombre']
            apellido = request.form['apellido']
            telefono = request.form['telefono']
            email = request.form['email']
            rol = mapear_rol(usuario.rol)  
            try:
                response = cliente_usuario.modificar_usuario(usuario.id, nombre_usuario, nombre, apellido, telefono, email, rol, session['token'])
                if response.status == "SUCCESS":
                    session['email'] = email
                    flash(response.message, "success")
                    return redirect(url_for('index'))
                else:
                    flash(response.message, "error")
            except Exception as e:
                print(f"Error al modificar cuenta: {e}")
                flash("Error al modificar cuenta", "error")
    
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
    return render_template('cuenta.html', usuario=usuario_dict)
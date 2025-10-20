# controllers/evento_controller.py
from flask import Blueprint, render_template, request, redirect, url_for, session, flash
from utils import requiere_autenticacion, requiere_rol_presidente_o_coordinador
from cliente_evento import ClienteEvento
from cliente_usuario import ClienteUsuario 

evento_bp = Blueprint('evento_bp', __name__)

# -----------------------
# Listar eventos
# -----------------------
@evento_bp.route('/eventos', methods=['GET'])
@requiere_autenticacion(ClienteUsuario())
def eventos():
    try:
        controller = ClienteEvento()
        response = controller.list_eventos(session['token'])
        eventos = response.eventos if response and hasattr(response, 'eventos') else []

        cliente_usuario = ClienteUsuario()
        usuario = cliente_usuario.traer_usuario_por_email(session['email'], session['token'])
        if usuario is None:
            session.clear()
            flash("Error al obtener datos del usuario", "error")
            return redirect(url_for('auth.index'))

        puede_administrar_eventos = usuario.rol in [0, 2]  # presidente o coordinador
        return render_template('eventos.html', eventos=eventos, puede_administrar_eventos=puede_administrar_eventos)

    except Exception as e:
        flash(f"Error al obtener eventos: {str(e)}", "error")
        return render_template('eventos.html', eventos=[])

# -----------------------
# Registrar evento
# -----------------------
@evento_bp.route('/eventos/registrar', methods=['GET', 'POST'])
@requiere_autenticacion(ClienteUsuario())
@requiere_rol_presidente_o_coordinador(ClienteUsuario())
def registrar_evento():
    if request.method == 'POST':
        nombre_evento = request.form['nombreEvento']
        descripcion = request.form['descripcion']
        fecha_hora = request.form['fechaHora']
        usuario_ids = [int(uid) for uid in request.form.getlist('usuarioIds') if uid]

        try:
            controller = ClienteEvento()
            response = controller.create_evento(
                nombre_evento=nombre_evento,
                descripcion=descripcion,
                fecha_hora=fecha_hora,
                usuario_ids=usuario_ids,
                token=session['token']
            )

            if response.status == "SUCCESS":
                flash("Evento registrado exitosamente", "success")
                return redirect(url_for('evento_bp.eventos'))
            else:
                flash(response.message or "Error al registrar evento", "error")
        except Exception as e:
            flash(f"Error al registrar evento: {str(e)}", "error")

    # Obtener lista de usuarios correctamente
    response_usuarios = ClienteUsuario().listar_usuarios(session['token'])
    usuarios = response_usuarios.usuarios if response_usuarios and hasattr(response_usuarios, 'usuarios') else []

    return render_template('registrar_evento.html', usuarios=usuarios)

# -----------------------
# Ver, modificar o eliminar evento
# -----------------------
@evento_bp.route('/eventos/<int:id_evento>', methods=['GET', 'POST'])
@requiere_autenticacion(ClienteEvento())
@requiere_rol_presidente_o_coordinador(ClienteUsuario())
def evento(id_evento):
    cliente_usuario = ClienteUsuario()
    usuario = cliente_usuario.traer_usuario_por_email(session['email'], session['token'])
    if not usuario:
        session.clear()
        flash("Error al obtener datos del usuario", "error")
        return redirect(url_for('auth.index'))

    controller = ClienteEvento()

    # Obtener lista de usuarios correctamente
    response_usuarios = ClienteUsuario().listar_usuarios(session['token'])
    usuarios = response_usuarios.usuarios if response_usuarios and hasattr(response_usuarios, 'usuarios') else []

    if request.method == 'POST':
        if 'eliminar' in request.form:
            try:
                response = controller.delete_evento(id_evento, session['token'])
                if response.success:
                    flash("Evento eliminado exitosamente", "success")
                    return redirect(url_for('evento_bp.eventos'))
                else:
                    flash(response.message, "error")
            except Exception as e:
                flash(f"Error al eliminar evento: {str(e)}", "error")
        else:
            nombre_evento = request.form['nombreEvento']
            descripcion = request.form['descripcion']
            fecha_hora = request.form['fechaHora']
            usuario_ids = [int(uid.strip()) for uid in request.form.get('usuarioIds', '').split(',') if uid.strip()]

            try:
                response = controller.update_evento(
                    id_evento=id_evento,
                    nombre_evento=nombre_evento,
                    descripcion=descripcion,
                    fecha_hora=fecha_hora,
                    usuario_ids=usuario_ids,
                    token=session['token']
                )
                if response.status == "SUCCESS":
                    flash("Evento actualizado exitosamente", "success")
                    return redirect(url_for('evento_bp.eventos'))
                else:
                    flash(response.message, "error")
            except Exception as e:
                flash(f"Error al actualizar evento: {str(e)}", "error")

    response = controller.get_evento(id_evento, session['token'])
    if response is None or response.status != "SUCCESS":
        flash(response.message if response else "Error al obtener el evento", "error")
        return redirect(url_for('evento_bp.eventos'))

    evento_data = response.evento
    puede_administrar_eventos = usuario.rol in [0, 2]

    return render_template('modificar_eliminar_evento.html',evento=evento_data, usuarios=usuarios, puede_administrar_eventos=puede_administrar_eventos)

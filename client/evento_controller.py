# controllers/evento_controller.py
from flask import Blueprint, render_template, request, redirect, url_for, session, flash
from utils import requiere_autenticacion, requiere_rol_presidente_o_coordinador, requiere_rol_voluntario
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
        es_voluntario = usuario.rol == 3  # voluntario (Role.VOLUNTARIO = 3)
        usuario_id = usuario.id  # Usar id

        # Obtener datos completos de cada evento
        eventos_completos = []
        for evento in eventos:
            response_evento = controller.get_evento(evento.idEvento, session['token'])
            if response_evento and response_evento.status == "SUCCESS":
                evento_completo = response_evento.evento
                # Convertir evento a diccionario
                evento_dict = {
                    'idEvento': evento_completo.idEvento,
                    'nombreEvento': evento_completo.nombreEvento,
                    'descripcion': evento_completo.descripcion,
                    'fechaHora': evento_completo.fechaHora,
                    'usuarioIds': [usuario.id for usuario in getattr(evento_completo, 'usuarios', [])]
                }
                eventos_completos.append(evento_dict)
                print(f"Evento {evento_dict['idEvento']}: usuarios={evento_dict['usuarioIds']}")
            else:
                print(f"Error al obtener evento {evento.idEvento}: {response_evento.message if response_evento else 'Respuesta nula'}")
                # Crear diccionario para el evento con usuarioIds vacío
                evento_dict = {
                    'idEvento': evento.idEvento,
                    'nombreEvento': evento.nombreEvento,
                    'descripcion': evento.descripcion,
                    'fechaHora': evento.fechaHora,
                    'usuarioIds': []
                }
                eventos_completos.append(evento_dict)

        # Depuración
        print(f"Usuario: email={session['email']}, rol={usuario.rol}, id={usuario.id}, es_voluntario={es_voluntario}")
        for evento in eventos_completos:
            print(f"Evento {evento['idEvento']}: nombre={evento['nombreEvento']}, usuarioIds={evento['usuarioIds']}")

        return render_template(
            'eventos.html',
            eventos=eventos_completos,
            puede_administrar_eventos=puede_administrar_eventos,
            es_voluntario=es_voluntario,
            usuario_id=usuario_id
        )

    except Exception as e:
        print(f"Error en eventos: {str(e)}")
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


@evento_bp.route('/eventos/publicar', methods=['GET', 'POST'])
@requiere_autenticacion(ClienteUsuario())
@requiere_rol_presidente_o_coordinador(ClienteUsuario())
def publicar_evento():
    if request.method == 'POST':
        id_organizacion = request.form['organizacionId']
        id_evento = request.form['eventoId']
        nombre_evento = request.form['nombreEvento']
        descripcion = request.form['descripcion']
        fecha_hora = request.form['fechaHora']

        try:
            cliente_evento = ClienteEvento()
            response = cliente_evento.publicar_evento(
                token=session['token'],
                id_evento=id_evento,
                nombre_evento=nombre_evento,
                descripcion=descripcion,
                fecha_hora=fecha_hora,
                id_organizacion=id_organizacion
            )

            if response and response.status == "SUCCESS":
              flash("Evento publicado correctamente", "success")
              return render_template('publicar_eventos.html',
                           organizacionId="",
                           eventoId="",
                           nombreEvento="",
                           descripcion="",
                           fechaHora="")
            else:
                flash(getattr(response, 'message', "Error al publicar el evento"), "error")

        except Exception as e:
            flash(f"Error: {str(e)}", "error")

    return render_template('publicar_eventos.html')

@evento_bp.route('/eventos-externos')
def listar_eventos_externos():
    token = session.get('token')  
    try:
        
        from app import cliente_evento

        response = cliente_evento.listar_eventos_externos(token)

        if response and response.status == "SUCCESS":
            eventos_externos = response.eventos
        else:
            flash(getattr(response, 'message', "Error al obtener eventos externos"), 'error')
            eventos_externos = []

    except Exception as e:
        flash(f"Error al obtener eventos externos: {e}", 'error')
        eventos_externos = []

    return render_template('eventos_externos.html', eventos=eventos_externos)


@evento_bp.route('/eventos/asignarse/<int:id_evento>', methods=['POST'])
@requiere_autenticacion(ClienteUsuario())
@requiere_rol_voluntario(ClienteUsuario())
def asignarse_evento(id_evento):
    try:
        cliente_usuario = ClienteUsuario()
        usuario = cliente_usuario.traer_usuario_por_email(session['email'], session['token'])
        if usuario is None:
            session.clear()
            flash("Error al obtener datos del usuario", "error")
            return redirect(url_for('auth.index'))

        # Depuración: Imprimir el token de la sesión
        token = session.get('token')
        print(f"Token en sesión: {token}")

        if not token:
            flash("No hay token en la sesión. Por favor, inicia sesión nuevamente.", "error")
            session.clear()
            return redirect(url_for('auth.index'))

        controller = ClienteEvento()
        response = controller.asignarse_evento(id_evento, usuario.id, token)

        if response.status == "SUCCESS":
            flash("Te has asignado al evento exitosamente", "success")
        else:
            flash(response.message or "Error al asignarse al evento", "error")

        return redirect(url_for('evento_bp.eventos'))

    except Exception as e:
        flash(f"Error al asignarse al evento: {str(e)}", "error")
        return redirect(url_for('evento_bp.eventos'))
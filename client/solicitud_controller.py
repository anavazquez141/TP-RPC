# controllers/solicitud_controller.py
from flask import Blueprint, render_template, request, redirect, url_for, session, flash
from utils import requiere_autenticacion, requiere_rol_presidente_o_vocal
from cliente_donacion import ClienteDonacion
from cliente_usuario import ClienteUsuario
from proto import donacionService_pb2 as donacion_pb2
import grpc
cliente_usuario = ClienteUsuario() 
solicitud_bp = Blueprint('solicitud_bp', __name__)

# Formulario para crear solicitud
@solicitud_bp.route('/form-solicitud')
def form_solicitud():
    return render_template('publicar_solicitud.html')


@solicitud_bp.route('/solicitudes', methods=['GET'])
@requiere_autenticacion(cliente_usuario)
@requiere_rol_presidente_o_vocal(cliente_usuario)
def solicitudes():
    try:
        token = session.get('token')
        if not token:
            flash("Debes iniciar sesión primero", "error")
            return redirect(url_for('auth_bp.login'))

        controller = ClienteDonacion()
        response = controller.listar_solicitudes(token)
        solicitudes = []

        if response and hasattr(response, 'solicitudes'):
            solicitudes = response.solicitudes
        else:
            flash("No se encontraron solicitudes", "error")

        return render_template('solicitudes.html', solicitudes=solicitudes)
    
    except Exception as e:
        flash(f"Error: {str(e)}", "error")
        return render_template('solicitudes.html', solicitudes=[])


@solicitud_bp.route('/solicitudes/baja/<id_solicitud>', methods=['POST'])
@requiere_autenticacion(cliente_usuario)
@requiere_rol_presidente_o_vocal(cliente_usuario)
def baja_solicitud(id_solicitud):
    try:
        token = session.get('token')
        if not token:
            flash("Debes iniciar sesión primero", "error")
            return redirect(url_for('auth_bp.login'))

        controller = ClienteDonacion()
        response = controller.listar_solicitudes(token)
        solicitud = next((s for s in response.solicitudes if s.id_solicitud == id_solicitud), None)

        if not solicitud:
            flash("Solicitud no encontrada", "error")
            return redirect(url_for('solicitud_bp.solicitudes'))

        response = controller.baja_solicitud_donacion(
            token=token,
            id_organizacion=solicitud.id_organizacion,
            id_solicitud=id_solicitud
        )

        if response and hasattr(response, 'status') and response.status == "SUCCESS":
            flash("Solicitud dada de baja exitosamente", "success")
        else:
            message = getattr(response, 'message', 'Error al dar de baja solicitud') if response else "Error al dar de baja solicitud"
            flash(message, "error")

    except Exception as e:
        flash(f"Error al dar de baja solicitud: {str(e)}", "error")

    return redirect(url_for('solicitud_bp.solicitudes'))


@solicitud_bp.route('/enviar-solicitud', methods=['POST'])
def enviar_solicitud():
    try:
        id_organizacion = request.form.get('id_organizacion')
        id_solicitud = request.form.get('id_solicitud')
        categorias = request.form.getlist('categoria[]')
        descripciones = request.form.getlist('descripcion[]')

        if not id_organizacion or not id_solicitud:
            flash("ID de organización y solicitud son obligatorios", "error")
            return redirect(url_for('solicitud_bp.form_solicitud'))

        if not categorias or not descripciones or len(categorias) != len(descripciones):
            flash("Debe proporcionar al menos un ítem válido con categoría y descripción", "error")
            return redirect(url_for('solicitud_bp.form_solicitud'))

        items = []
        for categoria, descripcion in zip(categorias, descripciones):
            item = donacion_pb2.ItemDonacionP(
                categoria=categoria,
                descripcion=descripcion
            )
            items.append(item)

        controller = ClienteDonacion()
        response = controller.solicitar_donacion(id_organizacion, id_solicitud, items)

        if response and hasattr(response, 'status') and response.status == "SUCCESS":
            flash("Solicitud publicada exitosamente", "success")
        else:
            message = getattr(response, 'message', 'Error al publicar solicitud') if response else "Error al publicar solicitud"
            flash(message, "error")

    except grpc.RpcError as e:
        flash(f"Error gRPC: {e.code().name} - {e.details()}", "error")
    except Exception as e:
        flash(f"Error al publicar solicitud: {str(e)}", "error")

    return redirect(url_for('solicitud_bp.form_solicitud'))

# controllers/donacion_controller.py
from flask import Blueprint, render_template, request, redirect, url_for, session, flash
from utils import requiere_autenticacion, requiere_rol_presidente_o_vocal
from cliente_usuario import ClienteUsuario
from cliente_donacion import ClienteDonacion
from proto import donacionService_pb2 as donacion_pb2

cliente_usuario = ClienteUsuario() 
donacion_bp = Blueprint('donacion_bp', __name__)

# Listar donaciones
@donacion_bp.route('/donaciones', methods=['GET'])
@requiere_autenticacion(cliente_usuario)
@requiere_rol_presidente_o_vocal(cliente_usuario)
def donaciones():
    try:
        token = session.get('token')
        if not token:
            flash("Debes iniciar sesión primero", "error")
            return redirect(url_for('auth_bp.login'))

        controller = ClienteDonacion()
        response = controller.listar_donaciones(token)
        donaciones = response.donaciones if response and hasattr(response, 'donaciones') else []
        if not donaciones:
            flash("No se encontraron donaciones", "error")

        return render_template('donaciones.html', donaciones=donaciones)

    except Exception as e:
        flash(f"Error: {str(e)}", "error")
        return render_template('donaciones.html', donaciones=[])

# Agregar donación
@donacion_bp.route('/agregar_donacion', methods=['GET', 'POST'])
@requiere_autenticacion(cliente_usuario)
@requiere_rol_presidente_o_vocal(cliente_usuario)
def agregar_donacion():
    if request.method == 'POST':
        try:
            token = session.get('token')
            if not token:
                flash("Debes iniciar sesión primero", "error")
                return redirect(url_for('auth_bp.login'))

            categoria = request.form['categoria']
            descripcion = request.form['descripcion']
            cantidad = int(request.form['cantidad'])

            if cantidad <= 0:
                flash("La cantidad debe ser mayor a 0", "error")
                return render_template('agregar_donacion.html')

            controller = ClienteDonacion()
            response = controller.registrar_donacion(token, categoria, descripcion, cantidad)

            if response and hasattr(response, 'status') and response.status == "SUCCESS":
                flash("Donación registrada exitosamente", "success")
                return redirect(url_for('donacion_bp.donaciones'))
            else:
                message = getattr(response, 'message', 'Error al registrar donación') if response else "Error al registrar donación"
                flash(message, "error")
                return render_template('agregar_donacion.html')

        except ValueError:
            flash("La cantidad debe ser un número válido", "error")
            return render_template('agregar_donacion.html')
        except Exception as e:
            flash(f"Error al registrar donación: {str(e)}", "error")
            return render_template('agregar_donacion.html')

    return render_template('agregar_donacion.html')

# Eliminar donación
@donacion_bp.route('/eliminar_donacion/<int:donacion_id>', methods=['POST'])
@requiere_autenticacion(cliente_usuario)
@requiere_rol_presidente_o_vocal(cliente_usuario)
def eliminar_donacion(donacion_id):
    try:
        token = session.get('token')
        if not token:
            flash("Debes iniciar sesión primero", "error")
            return redirect(url_for('auth_bp.login'))

        controller = ClienteDonacion()
        response = controller.eliminar_donacion(token, donacion_id)

        if response and hasattr(response, 'success') and response.success:
            flash("Donación eliminada exitosamente", "success")
        else:
            message = getattr(response, 'message', 'Error al eliminar donación') if response else "Error al eliminar donación"
            flash(message, "error")

    except Exception as e:
        flash(f"Error al eliminar donación: {str(e)}", "error")

    return redirect(url_for('donacion_bp.donaciones'))

# Modificar donación
@donacion_bp.route('/modificar_donacion/<int:donacion_id>', methods=['GET', 'POST'])
@requiere_autenticacion(cliente_usuario)
@requiere_rol_presidente_o_vocal(cliente_usuario)
def modificar_donacion(donacion_id):
    controller = ClienteDonacion()
    token = session.get('token')
    if not token:
        flash("Debes iniciar sesión primero", "error")
        return redirect(url_for('auth_bp.login'))

    if request.method == 'POST':
        try:
            descripcion = request.form['descripcion']
            cantidad = int(request.form['cantidad'])

            if cantidad <= 0:
                flash("La cantidad debe ser mayor a 0", "error")
                return redirect(url_for('donacion_bp.modificar_donacion', donacion_id=donacion_id))

            response = controller.modificar_donacion(token, donacion_id, descripcion, cantidad)

            if response and hasattr(response, 'status') and response.status == "SUCCESS":
                flash("Donación modificada exitosamente", "success")
                return redirect(url_for('donacion_bp.donaciones'))
            else:
                message = getattr(response, 'message', 'Error al modificar donación') if response else "Error al modificar donación"
                flash(message, "error")
                return redirect(url_for('donacion_bp.modificar_donacion', donacion_id=donacion_id))

        except ValueError:
            flash("La cantidad debe ser un número válido", "error")
            return redirect(url_for('donacion_bp.modificar_donacion', donacion_id=donacion_id))
        except Exception as e:
            flash(f"Error al modificar donación: {str(e)}", "error")
            return redirect(url_for('donacion_bp.modificar_donacion', donacion_id=donacion_id))

    try:
        response = controller.traer_donacion_por_id(token, donacion_id)
        if response and hasattr(response, 'status') and response.status == "SUCCESS":
            donacion = response
            return render_template('modificar_donacion.html', donacion=donacion)
        else:
            flash("Error al cargar la donación", "error")
            return redirect(url_for('donacion_bp.donaciones'))
    except Exception as e:
        flash(f"Error: {str(e)}", "error")
        return redirect(url_for('donacion_bp.donaciones'))

# controllers/donacion_controller.py
from flask import Blueprint, jsonify, render_template, request, redirect, url_for, session, flash, send_file
from utils import requiere_autenticacion, requiere_rol_presidente_o_vocal
from cliente_usuario import ClienteUsuario
from cliente_donacion import ClienteDonacion
from cliente_donacion_graph import ClienteDonacionGraph
from proto import donacionService_pb2 as donacion_pb2
import grpc
import requests
import io
from openpyxl import Workbook

cliente_usuario = ClienteUsuario() 
cliente_donacion = ClienteDonacion() 
donacion_bp = Blueprint('donacion_bp', __name__)

# ------------------ DONACIONES ------------------

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

# ------------------ AGREGAR DONACIÓN ------------------

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

# ------------------ ELIMINAR DONACIÓN ------------------

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

# ------------------ MODIFICAR DONACIÓN ------------------

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

# ------------------ INFORME DE DONACIONES ------------------

@donacion_bp.route('/informe_donaciones', methods=['GET', 'POST'])
@requiere_autenticacion(cliente_usuario)
@requiere_rol_presidente_o_vocal(cliente_usuario)
def informe_donaciones():
    token = session.get('token')
    if not token:
        flash("Debes iniciar sesión primero", "error")
        return redirect(url_for('auth_bp.login'))

    categoria = request.values.get('categoria') or None
    if categoria:
        categoria = categoria.upper()

    fecha_desde = request.values.get('fechaDesde') or None
    fecha_hasta = request.values.get('fechaHasta') or None
    eliminado_str = request.values.get('eliminado')
    if eliminado_str == 'si':
        eliminado = True
    elif eliminado_str == 'no':
        eliminado = False
    else:
        eliminado = None

    filtros_aplicados = any([categoria, fecha_desde, fecha_hasta, eliminado is not None])
    informe = []

    cliente = ClienteDonacionGraph()

    if filtros_aplicados:
        informe = cliente.informe_donaciones_filtradas(
            token=token,
            categoria=categoria,
            fecha_desde=fecha_desde,
            fecha_hasta=fecha_hasta,
            eliminado=eliminado
        )

        if not informe:
            flash("No se encontraron donaciones para mostrar", "info")

    # Traer filtros guardados
    filtros_guardados = cliente.traer_filtros(token)

    return render_template('informe_donaciones.html', informe=informe, filtros_guardados=filtros_guardados)

# ------------------ FILTROS GUARDADOS ------------------

@donacion_bp.route('/guardar_filtro', methods=['POST'])
@requiere_autenticacion(cliente_usuario)
@requiere_rol_presidente_o_vocal(cliente_usuario)
def guardar_filtro():
    token = session.get('token')
    if not token:
        flash("Debes iniciar sesión primero", "error")
        return redirect(url_for('auth_bp.login'))

    nombre_filtro = request.form.get('nombreFiltro')
    categoria = request.form.get('categoria') or None
    fecha_desde = request.form.get('fechaDesde') or None
    fecha_hasta = request.form.get('fechaHasta') or None
    eliminado_str = request.form.get('eliminado')
    if eliminado_str == 'si':
        eliminado = True
    elif eliminado_str == 'no':
        eliminado = False
    else:
        eliminado = None

    try:
        cliente = ClienteDonacionGraph()
        cliente.guardar_filtro(token, nombre_filtro, categoria, fecha_desde, fecha_hasta, eliminado)
        flash(f"Filtro '{nombre_filtro}' guardado correctamente", "success")
    except Exception as e:
        flash(f"Error al guardar filtro: {str(e)}", "error")

    return redirect(url_for('donacion_bp.informe_donaciones'))

@donacion_bp.route('/aplicar_filtro', methods=['POST'])
@requiere_autenticacion(cliente_usuario)
@requiere_rol_presidente_o_vocal(cliente_usuario)
def aplicar_filtro():
    token = session.get('token')
    if not token:
        flash("Debes iniciar sesión primero", "error")
        return redirect(url_for('auth_bp.login'))

    filtro_id = request.form.get('filtroId')
    try:
        cliente = ClienteDonacionGraph()
        filtro = cliente.traer_filtro_por_id(token, filtro_id)

        # Extraemos valores del dict con get(), por si son None
        categoria = filtro.get('categoria') or ''
        fecha_desde = filtro.get('fechaDesde') or ''
        fecha_hasta = filtro.get('fechaHasta') or ''
        eliminado = filtro.get('eliminado')
        if eliminado is True:
            eliminado_str = 'si'
        elif eliminado is False:
            eliminado_str = 'no'
        else:
            eliminado_str = ''

        return redirect(url_for(
            'donacion_bp.informe_donaciones',
            categoria=categoria,
            fechaDesde=fecha_desde,
            fechaHasta=fecha_hasta,
            eliminado=eliminado_str
        ))
    except Exception as e:
        flash(f"Error al aplicar filtro: {str(e)}", "error")
        return redirect(url_for('donacion_bp.informe_donaciones'))




@donacion_bp.route('/eliminar_filtro', methods=['POST'])
@requiere_autenticacion(cliente_usuario)
@requiere_rol_presidente_o_vocal(cliente_usuario)
def eliminar_filtro():
    token = session.get('token')
    if not token:
        flash("Debes iniciar sesión primero", "error")
        return redirect(url_for('auth_bp.login'))

    filtro_id = request.form.get('filtroId')
    try:
        cliente = ClienteDonacionGraph()
        cliente.eliminar_filtro(token, filtro_id)
        flash("Filtro eliminado correctamente", "success")
    except Exception as e:
        flash(f"Error al eliminar filtro: {str(e)}", "error")

    return redirect(url_for('donacion_bp.informe_donaciones'))


@donacion_bp.route('/transferir_donaciones', methods=['GET', 'POST'])
@requiere_autenticacion(cliente_usuario)
@requiere_rol_presidente_o_vocal(cliente_usuario)
def transferir_donaciones():
    token = session.get('token')
    if not token:
        flash("Debes iniciar sesión primero", "error")
        return redirect(url_for('auth_bp.login'))

    controller = ClienteDonacion()  

    if request.method == "POST":
        id_solicitud = request.form.get('id_solicitud')
        id_organizacion_solicitante = request.form.get('id_organizacion_solicitante')
        categorias = request.form.getlist('categoria[]')
        descripciones = request.form.getlist('descripcion[]')
        cantidades = request.form.getlist('cantidad[]')

        if not id_solicitud or not id_organizacion_solicitante:
            flash("Debes completar ID de solicitud y ID de organización", "error")
            return redirect(url_for('donacion_bp.transferir_donaciones'))

        if not categorias or not descripciones or not cantidades or len(categorias) != len(descripciones) or len(categorias) != len(cantidades):
            flash("Debe completar al menos un item con categoría, descripción y cantidad", "error")
            return redirect(url_for('donacion_bp.transferir_donaciones'))

        # Construir la lista de objetos para gRPC
        items_form = []
        for cat, desc, cant in zip(categorias, descripciones, cantidades):
            try:
                items_form.append({
                    "categoria": cat,
                    "descripcion": desc,
                    "cantidad": int(cant)
                })
            except ValueError:
                flash("Cantidad inválida en algún item", "error")
                return redirect(url_for('donacion_bp.transferir_donaciones'))

        # Llamada gRPC
        response = controller.transferir_donacion(id_organizacion_solicitante, id_solicitud, items_form)
        if response and hasattr(response, 'status') and response.status == "SUCCESS":
            flash("Donaciones transferidas correctamente", "success")
        else:
            flash("Error al transferir donaciones", "error")

        return redirect(url_for('donacion_bp.donaciones'))

    # GET → listar donaciones disponibles
    response = controller.listar_donaciones(token)
    donaciones = response.donaciones if response and hasattr(response, 'donaciones') else []

    return render_template('transferir_donaciones.html', donaciones=donaciones)

@donacion_bp.route('/descargar_excel', methods=['GET'])
@requiere_autenticacion(cliente_usuario)
@requiere_rol_presidente_o_vocal(cliente_usuario)
def descargar_excel():
    token = session.get('token')
    if not token:
        flash("Debes iniciar sesión primero", "error")
        return redirect(url_for('auth_bp.login'))

    categoria = request.args.get('categoria', '')
    fecha_desde = request.args.get('fechaDesde', '')
    fecha_hasta = request.args.get('fechaHasta', '')
    eliminado = request.args.get('eliminado', '')

    url_rest = f"http://localhost:8050/api/informes/donaciones/descargar_excel?categoria={categoria}&fechaDesde={fecha_desde}&fechaHasta={fecha_hasta}&eliminado={eliminado}"

    try:
        headers = {'Authorization': f'Bearer {token}'}
        print(f"Enviando solicitud a: {url_rest}")
        resp = requests.get(url_rest, headers=headers, stream=True)
        resp.raise_for_status()

        content_type = resp.headers.get('Content-Type')
        print(f"Content-Type recibido: {content_type}")
        if not ('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' in content_type or
                'application/octet-stream' in content_type):
            print(f"Respuesta no es un Excel: {resp.text[:200]}")
            raise Exception(f"Respuesta no es un archivo Excel, Content-Type: {content_type}")

        return send_file(
            io.BytesIO(resp.content),
            mimetype='application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
            download_name='informe_donaciones.xlsx',
            as_attachment=True
        )

    except requests.RequestException as e:
        print(f"Error al descargar el Excel: {str(e)}")
        flash(f"Error al descargar el Excel: {str(e)}", "error")
        return redirect(url_for('donacion_bp.informe_donaciones'))


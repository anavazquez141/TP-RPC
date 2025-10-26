# controllers/oferta_controller.py
from flask import Blueprint, render_template, request, redirect, url_for, session, flash
from utils import requiere_autenticacion, requiere_rol_presidente_o_vocal
from cliente_donacion import ClienteDonacion
from cliente_usuario import ClienteUsuario
from proto import donacionService_pb2 as donacion_pb2

oferta_bp = Blueprint('oferta_bp', __name__)

# Formulario para crear oferta
@oferta_bp.route('/form-oferta')
def form_oferta():
    return render_template('ofrecer_donacion.html')


@oferta_bp.route('/ofertas', methods=['GET'])
@requiere_autenticacion(ClienteUsuario())
@requiere_rol_presidente_o_vocal(ClienteUsuario())
def ofertas():
    try:
        token = session.get('token')
        if not token:
            flash("Debes iniciar sesión primero", "error")
            return redirect(url_for('auth_bp.login'))

        controller = ClienteDonacion()
        response = controller.listar_ofertas(token)
        ofertas = []

        if response and hasattr(response, 'ofertas'):
            for o in response.ofertas:
                oferta_dict = {
                    'id_oferta': o.idOferta,
                    'id_organizacion': o.idOrganizacion,
                    'lista_items': [{'categoria': i.categoria, 'descripcion': i.descripcion, 'cantidad': i.cantidad} 
                                     for i in o.items]
                }
                ofertas.append(oferta_dict)
        else:
            flash("No se encontraron ofertas", "error")

        return render_template('ofertas.html', ofertas=ofertas)
    
    except Exception as e:
        flash(f"Error: {str(e)}", "error")
        return render_template('ofertas.html', ofertas=[])


@oferta_bp.route('/enviar-oferta', methods=['POST'])
def enviar_oferta():
    try:
        id_organizacion = request.form.get('id_organizacion')
        id_oferta = request.form.get('id_oferta')
        categorias = request.form.getlist('categoria[]')
        descripciones = request.form.getlist('descripcion[]')
        cantidades = request.form.getlist('cantidad[]')

        if not id_organizacion or not id_oferta:
            flash("ID de organización y oferta son obligatorios", "error")
            return redirect(url_for('oferta_bp.form_oferta'))

        if not categorias or not descripciones or not cantidades:
            flash("Debe agregar al menos una donación", "error")
            return redirect(url_for('oferta_bp.form_oferta'))

        if len(categorias) != len(descripciones) or len(categorias) != len(cantidades):
            flash("Los datos de las donaciones no coinciden", "error")
            return redirect(url_for('oferta_bp.form_oferta'))

        items = []
        for cat, desc, cant in zip(categorias, descripciones, cantidades):
            try:
                cantidad_int = int(cant)
                if cantidad_int <= 0:
                    flash("La cantidad debe ser mayor a 0", "error")
                    return redirect(url_for('oferta_bp.form_oferta'))
            except ValueError:
                flash("La cantidad debe ser un número válido", "error")
                return redirect(url_for('oferta_bp.form_oferta'))

            item = donacion_pb2.OfertaDonacionItem(
                categoria=cat,
                descripcion=desc,
                cantidad=cantidad_int
            )
            items.append(item)

        controller = ClienteDonacion()
        response = controller.ofrecer_donacion(id_organizacion, id_oferta, items)

        if response and hasattr(response, 'status') and response.status == "SUCCESS":
            flash("Oferta publicada exitosamente", "success")
        else:
            message = getattr(response, 'message', 'Error al publicar oferta') if response else "Error al publicar oferta"
            flash(message, "error")

    except Exception as e:
        flash(f"Error al publicar oferta: {str(e)}", "error")

    return redirect(url_for('oferta_bp.form_oferta'))

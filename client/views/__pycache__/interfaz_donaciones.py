from flask import render_template, request, redirect, url_for, session, flash
from controllers.donacion_controller import DonacionController

class InterfazDonaciones:
    #Lista de donaciones
    @staticmethod
    def listar_donaciones():
        try:
            token = session.get('token')
            if not token:
                flash("Debes iniciar sesión primero", "error")
                return redirect(url_for('login'))
            
            controller = DonacionController()
            response = controller.listar_donaciones(token)
            
            if response and hasattr(response, 'donaciones'):
                donaciones = response.donaciones
            else:
                donaciones = []
                flash("Error al obtener las donaciones", "error")
            
            return render_template('donaciones.html', donaciones=donaciones)
        except Exception as e:
            flash(f"Error: {str(e)}", "error")
            return render_template('donaciones.html', donaciones=[])

    #Agregar donación
    @staticmethod
    def agregar_donacion():
        if request.method == 'POST':
            try:
                token = session.get('token')
                if not token:
                    flash("Debes iniciar sesión primero", "error")
                    return redirect(url_for('login'))
                
                categoria = request.form['categoria']
                descripcion = request.form['descripcion']
                cantidad = int(request.form['cantidad'])
                
                if cantidad <= 0:
                    flash("La cantidad debe ser mayor a 0", "error")
                    return render_template('agregar_donacion.html')
                
                controller = DonacionController()
                response = controller.registrar_donacion(token, categoria, descripcion, cantidad)
                
                if response and hasattr(response, 'status') and response.status == "SUCCESS":
                    flash("Donación registrada exitosamente", "success")
                    return redirect(url_for('donaciones'))
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

    #Eliminar una donación
    @staticmethod
    def eliminar_donacion(donacion_id):
        try:
            token = session.get('token')
            if not token:
                flash("Debes iniciar sesión primero", "error")
                return redirect(url_for('login'))
            
            controller = DonacionController()
            response = controller.eliminar_donacion(token, donacion_id)
            
            if response and hasattr(response, 'success') and response.success:
                flash("Donación eliminada exitosamente", "success")
            else:
                message = getattr(response, 'message', 'Error al eliminar donación') if response else "Error al eliminar donación"
                flash(message, "error")
                
        except Exception as e:
            flash(f"Error al eliminar donación: {str(e)}", "error")
        
        return redirect(url_for('donaciones'))

    #Modificar donación
    @staticmethod
    def modificar_donacion(donacion_id):
        if request.method == 'POST':
            try:
                token = session.get('token')
                if not token:
                    flash("Debes iniciar sesión primero", "error")
                    return redirect(url_for('login'))
                
                descripcion = request.form['descripcion']
                cantidad = int(request.form['cantidad'])
                
                # Validaciones
                if cantidad <= 0:
                    flash("La cantidad debe ser mayor a 0", "error")
                    return redirect(url_for('modificar_donacion', donacion_id=donacion_id))
                
                controller = DonacionController()
                response = controller.modificar_donacion(token, donacion_id, descripcion, cantidad)
                
                if response and hasattr(response, 'status') and response.status == "SUCCESS":
                    flash("Donación modificada exitosamente", "success")
                    return redirect(url_for('donaciones'))
                else:
                    message = getattr(response, 'message', 'Error al modificar donación') if response else "Error al modificar donación"
                    flash(message, "error")
                    return redirect(url_for('modificar_donacion', donacion_id=donacion_id))
                    
            except ValueError:
                flash("La cantidad debe ser un número válido", "error")
                return redirect(url_for('modificar_donacion', donacion_id=donacion_id))
            except Exception as e:
                flash(f"Error al modificar donación: {str(e)}", "error")
                return redirect(url_for('modificar_donacion', donacion_id=donacion_id))
        try:
            token = session.get('token')
            if not token:
                flash("Debes iniciar sesión primero", "error")
                return redirect(url_for('login'))
            
            controller = DonacionController()
            response = controller.traer_donacion_por_id(token, donacion_id)
            
            if response and hasattr(response, 'status') and response.status == "SUCCESS":
                donacion = response
                return render_template('modificar_donacion.html', donacion=donacion)
            else:
                flash("Error al cargar la donación", "error")
                return redirect(url_for('donaciones'))
                
        except Exception as e:
            flash(f"Error: {str(e)}", "error")
            return redirect(url_for('donaciones'))
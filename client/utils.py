# utils.py

from functools import wraps
from flask import session, flash, redirect, url_for

ROL_MAP = {
    0: "PRESIDENTE",
    1: "VOCAL",
    2: "COORDINADOR",
    3: "VOLUNTARIO"
}

def mapear_rol(valor_rol):
    return ROL_MAP.get(valor_rol, "Desconocido")


def requiere_autenticacion(cliente):
    def decorador(f):
        @wraps(f)
        def wrapper(*args, **kwargs):
            if 'token' not in session:
                session.clear()
                flash("Debes iniciar sesión primero", "error")
                return redirect(url_for('login'))
            
            response = cliente.validar_token(session['token'])
            if response is None or response.status != "SUCCESS":
                session.clear()
                flash("Sesión inválida, inicia sesión nuevamente", "error")
                return redirect(url_for('login'))
            
            return f(*args, **kwargs)
        return wrapper
    return decorador


def requiere_rol_presidente(cliente):
    def decorador(f):
        @wraps(f)
        def wrapper(*args, **kwargs):
            usuario = cliente.traer_usuario_por_email(session['email'], session['token'])
            if usuario is None:
                session.clear()
                flash("Error al obtener datos del usuario", "error")
                return redirect(url_for('login'))
            if usuario.rol != 0:
                flash("No tienes permisos para acceder a esta funcionalidad", "error")
                return redirect(url_for('index'))
            return f(*args, **kwargs)
        return wrapper
    return decorador
import tkinter as tk
from tkinter import messagebox
from proto import usuarioService_pb2 as usuario_pb2
from client.cliente_usuario import ClienteUsuario

# ------------------ Cliente gRPC ------------------
cliente = ClienteUsuario()

# ------------------ Funciones ------------------
def listar_usuarios():
    usuarios = cliente.listar_usuarios()
    listbox_usuarios.delete(0, tk.END)
    for u in usuarios.usuarios:
        rol_texto = usuario_pb2.Rol.Name(u.rol)
        activo_texto = "Sí" if u.activo else "No"
        listbox_usuarios.insert(tk.END, f"{u.id} | {u.nombreUsuario} | {u.nombre} | {u.apellido} | {u.telefono} | {u.email} | {rol_texto} | {activo_texto}")

def validar_campos():
    if not entry_usuario.get() or not entry_nombre.get() or not entry_email.get() or not entry_clave.get():
        messagebox.showerror("Error", "Completa todos los campos obligatorios (Usuario, Nombre, Email, Clave)")
        return False
    return True

def registrar_usuario():
    if not validar_campos():
        return
    rol_proto = usuario_pb2.Rol.Value(rol_seleccionado.get())
    datos = {
        "nombreUsuario": entry_usuario.get(),
        "nombre": entry_nombre.get(),
        "apellido": entry_apellido.get(),
        "telefono": entry_telefono.get(),
        "email": entry_email.get(),
        "rol": rol_proto,
        "activo": True,
        "clave": entry_clave.get()
    }
    resp = cliente.registrar_usuario(**datos)
    messagebox.showinfo("Info", f"Usuario registrado: {resp.id}")
    listar_usuarios()

def modificar_usuario():
    if not validar_campos():
        return
    try:
        user_id = int(entry_id.get())
    except ValueError:
        messagebox.showerror("Error", "ID inválido")
        return
    rol_proto = usuario_pb2.Rol.Value(rol_seleccionado.get())
    datos = {
        "nombreUsuario": entry_usuario.get(),
        "nombre": entry_nombre.get(),
        "apellido": entry_apellido.get(),
        "telefono": entry_telefono.get(),
        "email": entry_email.get(),
        "rol": rol_proto,
        "activo": True,
        "clave": entry_clave.get()
    }
    resp = cliente.modificar_usuario(user_id, **datos)
    messagebox.showinfo("Info", f"Usuario modificado: {resp.nombreUsuario}")
    listar_usuarios()

def eliminar_usuario():
    try:
        user_id = int(entry_id.get())
    except ValueError:
        messagebox.showerror("Error", "ID inválido")
        return
    resp = cliente.eliminar_usuario(user_id)
    messagebox.showinfo("Info", resp.message)
    listar_usuarios()

def traer_usuario_por_id():
    try:
        user_id = int(entry_id.get())
    except ValueError:
        messagebox.showerror("Error", "ID inválido")
        return
    usuario = cliente.traer_usuario_por_id(user_id)
    if usuario.message.startswith("Error"):
        messagebox.showerror("Error", usuario.message)
    else:
        entry_usuario.delete(0, tk.END)
        entry_usuario.insert(0, usuario.nombreUsuario)
        entry_nombre.delete(0, tk.END)
        entry_nombre.insert(0, usuario.nombre)
        entry_apellido.delete(0, tk.END)
        entry_apellido.insert(0, usuario.apellido)
        entry_telefono.delete(0, tk.END)
        entry_telefono.insert(0, usuario.telefono)
        entry_email.delete(0, tk.END)
        entry_email.insert(0, usuario.email)
        entry_clave.delete(0, tk.END)
        entry_clave.insert(0, usuario.clave if hasattr(usuario, 'clave') else "")
        rol_seleccionado.set(usuario_pb2.Rol.Name(usuario.rol) if hasattr(usuario, 'rol') else rol_seleccionado.get())

def on_seleccionar_usuario(event):
    seleccion = listbox_usuarios.curselection()
    if seleccion:
        index = seleccion[0]
        datos = listbox_usuarios.get(index).split(" | ")
        entry_id.delete(0, tk.END)
        entry_id.insert(0, datos[0])
        entry_usuario.delete(0, tk.END)
        entry_usuario.insert(0, datos[1])
        entry_nombre.delete(0, tk.END)
        entry_nombre.insert(0, datos[2])
        entry_apellido.delete(0, tk.END)
        entry_apellido.insert(0, datos[3])
        entry_telefono.delete(0, tk.END)
        entry_telefono.insert(0, datos[4])
        entry_email.delete(0, tk.END)
        entry_email.insert(0, datos[5])
        rol_seleccionado.set(datos[6])
        entry_clave.delete(0, tk.END)

# ------------------ Interfaz Tkinter ------------------
root = tk.Tk()
root.title("Cliente gRPC - Usuarios")
root.state('zoomed')

# ----- Frame de campos -----
frame_campos = tk.Frame(root, padx=10, pady=10)
frame_campos.pack(fill=tk.X)

labels = ["ID", "Usuario", "Nombre", "Apellido", "Teléfono", "Email", "Clave"]
entries = []
for i, text in enumerate(labels):
    tk.Label(frame_campos, text=text).grid(row=i, column=0, sticky=tk.W, pady=2)
    e = tk.Entry(frame_campos, width=40)
    e.grid(row=i, column=1, pady=2, padx=5)
    entries.append(e)

entry_id, entry_usuario, entry_nombre, entry_apellido, entry_telefono, entry_email, entry_clave = entries

# ----- Dropdown de Roles -----
roles = [usuario_pb2.Rol.VOLUNTARIO, usuario_pb2.Rol.PRESIDENTE, usuario_pb2.Rol.VOCAL, usuario_pb2.Rol.COORDINADOR]
roles_nombres = [usuario_pb2.Rol.Name(r) for r in roles]
rol_seleccionado = tk.StringVar()
rol_seleccionado.set(roles_nombres[0])
tk.Label(frame_campos, text="Rol").grid(row=len(labels), column=0, sticky=tk.W, pady=2)
dropdown_rol = tk.OptionMenu(frame_campos, rol_seleccionado, *roles_nombres)
dropdown_rol.grid(row=len(labels), column=1, pady=2, padx=5)

# ----- Frame de botones -----
frame_botones = tk.Frame(root, padx=10, pady=10)
frame_botones.pack(fill=tk.X)

tk.Button(frame_botones, text="Registrar", command=registrar_usuario).grid(row=0, column=0, padx=5)
tk.Button(frame_botones, text="Modificar", command=modificar_usuario).grid(row=0, column=1, padx=5)
tk.Button(frame_botones, text="Eliminar", command=eliminar_usuario).grid(row=0, column=2, padx=5)
tk.Button(frame_botones, text="Traer por ID", command=traer_usuario_por_id).grid(row=0, column=3, padx=5)
tk.Button(frame_botones, text="Listar todos", command=listar_usuarios).grid(row=0, column=4, padx=5)

# ----- Frame de lista -----
frame_lista = tk.Frame(root, padx=10, pady=10)
frame_lista.pack(fill=tk.BOTH, expand=True)

scrollbar = tk.Scrollbar(frame_lista)
scrollbar.pack(side=tk.RIGHT, fill=tk.Y)

listbox_usuarios = tk.Listbox(frame_lista, yscrollcommand=scrollbar.set)
listbox_usuarios.pack(fill=tk.BOTH, expand=True)
scrollbar.config(command=listbox_usuarios.yview)

listbox_usuarios.bind("<<ListboxSelect>>", on_seleccionar_usuario)

# Cargar usuarios al iniciar
listar_usuarios()

# ----- Ejecutar ventana -----
root.mainloop()

# Cerrar canal gRPC al cerrar la ventana
cliente.cerrar()
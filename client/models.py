from flask_sqlalchemy import SQLAlchemy

db = SQLAlchemy()

class OfertaDonacion(db.Model):
    __tablename__ = 'oferta_donacion'  # tabla existente
    id = db.Column(db.Integer, primary_key=True)
    id_organizacion = db.Column(db.Integer)
    id_oferta = db.Column(db.Integer)
    vigente = db.Column(db.Boolean)
    items = db.relationship('OfertaDonacionItem', backref='oferta', lazy=True)

class OfertaDonacionItem(db.Model):
    __tablename__ = 'oferta_donacion_items'  # tabla existente
    id = db.Column(db.Integer, primary_key=True)
    categoria = db.Column(db.String(100))
    descripcion = db.Column(db.String(255))
    cantidad = db.Column(db.Integer)
    oferta_id = db.Column(db.Integer, db.ForeignKey('oferta_donacion.id'))

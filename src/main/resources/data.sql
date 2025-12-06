-- ======================================
-- DATA.SQL — Datos iniciales del sistema
-- ======================================

-- Tipos de usuario
INSERT INTO tipo_usuario (tipo_usuario) VALUES
('cooperativa'),
('socio'),
('ingenio'),
('comercializadora'),
('transportista'),
('administrador')
ON CONFLICT DO NOTHING;

-- Minerales base
INSERT INTO minerales (nombre, nomenclatura) VALUES
                                                 ('Plata', 'Ag'),
                                                 ('Plomo', 'Pb'),
                                                 ('Zinc', 'Zn');

-- Procesos de planta
INSERT INTO procesos (nombre) VALUES
                                  ('Chancado'),
                                  ('Molienda'),
                                  ('Concentración'),
                                  ('Flotación'),
                                  ('Secado');
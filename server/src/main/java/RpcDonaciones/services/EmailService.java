package RpcDonaciones.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendWelcomeEmail(String to, String username, String password) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject("Bienvenido a Empuje Comunitario - Tus Credenciales de Acceso");
        helper.setFrom("empujecomunitariodonaciones@gmail.com");

        // Contenido del correo en HTML para mejor presentación
        String htmlContent = """
            <html>
            <body>
                <h2>¡Bienvenido a Empuje Comunitario!</h2>
                <p>Estimado/a <strong>%s</strong>,</p>
                <p>Tu cuenta ha sido creada exitosamente. A continuación, encontrarás tus credenciales de acceso:</p>
                <ul>
                    <li><strong>Nombre de usuario:</strong> %s</li>
                    <li><strong>Contraseña:</strong> %s</li>
                </ul>
                <p>Por favor, inicia sesión y cambia tu contraseña lo antes posible.</p>
                <p>Si tienes alguna duda, contáctanos en <a href="mailto:empujecomunitariodonaciones@gmail.com">empujecomunitariodonaciones@gmail.com</a>.</p>
                <p>¡Gracias por unirte a nuestra comunidad!</p>
                <p>Equipo RpcDonaciones</p>
            </body>
            </html>
            """.formatted(username, username, password);

        helper.setText(htmlContent, true); // true indica que el contenido es HTML

        mailSender.send(message);
    }
}
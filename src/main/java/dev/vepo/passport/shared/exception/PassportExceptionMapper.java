package dev.vepo.passport.shared.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class PassportExceptionMapper implements ExceptionMapper<Exception> {
    private static final Logger logger = LoggerFactory.getLogger(PassportExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {
        logger.error("An errro happen!", exception);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                       .type(MediaType.APPLICATION_JSON)
                       .entity(new ErrorResponse(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                                                 exception.getMessage()))
                       .build();
    }

}

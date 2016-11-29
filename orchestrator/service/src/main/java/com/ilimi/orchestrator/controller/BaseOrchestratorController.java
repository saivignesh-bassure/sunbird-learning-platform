package com.ilimi.orchestrator.controller;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ilimi.common.dto.Response;
import com.ilimi.common.dto.ResponseParams;
import com.ilimi.common.dto.ResponseParams.StatusType;
import com.ilimi.common.exception.ClientException;
import com.ilimi.common.exception.MiddlewareException;
import com.ilimi.common.exception.ResourceNotFoundException;
import com.ilimi.common.exception.ResponseCode;
import com.ilimi.orchestrator.dac.model.OrchestratorScript;
import com.ilimi.orchestrator.interpreter.exception.ExecutionErrorCodes;

public abstract class BaseOrchestratorController {
    
    private static final String API_ID_PREFIX = "orchestrator";
    private static final String API_VERSION = "2.0";
    
    protected ObjectMapper mapper = new ObjectMapper();
    
    protected Response getSuccessResponse() {
        Response response = new Response();
        ResponseParams params = new ResponseParams();
        params.setErr("0");
        params.setStatus(StatusType.successful.name());
        params.setErrmsg("Operation successful");
        response.setParams(params);
        return response;
    }

    protected ResponseEntity<Response> getResponseEntity(Response response, OrchestratorScript script) {
        int statusCode = response.getResponseCode().code();
        HttpStatus status = getStatus(statusCode);
        String apiId = StringUtils.isBlank(script.getApiId()) ? script.getName() : script.getApiId();
        setResponseEnvelope(response, null, apiId, script.getVersion());
        return new ResponseEntity<Response>(response, status);
    }
    
    protected ResponseEntity<Response> getResponseEntity(Response response, String apiId) {
        int statusCode = response.getResponseCode().code();
        HttpStatus status = getStatus(statusCode);
        setResponseEnvelope(response, API_ID_PREFIX, apiId, null);
        return new ResponseEntity<Response>(response, status);
    }

    protected Response getErrorResponse(Exception e) {
        Response response = new Response();
        ResponseParams resStatus = new ResponseParams();
        resStatus.setErrmsg(e.getMessage());
        resStatus.setStatus(StatusType.failed.name());
        if (e instanceof MiddlewareException) {
            MiddlewareException me = (MiddlewareException) e;
            resStatus.setErr(me.getErrCode());
            response.setResponseCode(me.getResponseCode());
        } else {
            resStatus.setErr(ExecutionErrorCodes.ERR_SYSTEM_ERROR.name());
            response.setResponseCode(ResponseCode.SERVER_ERROR);
        }
        response.setParams(resStatus);
        return response;
    }

    protected HttpStatus getHttpStatus(Exception e) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (e instanceof ClientException) {
            status = HttpStatus.BAD_REQUEST;
        } else if (e instanceof ResourceNotFoundException) {
            status = HttpStatus.NOT_FOUND;
        }
        return status;
    }

    protected ResponseEntity<Response> getExceptionResponseEntity(Exception e, OrchestratorScript script) {
        HttpStatus status = getHttpStatus(e);
        Response response = getErrorResponse(e);
        String apiId = StringUtils.isBlank(script.getApiId()) ? script.getName() : script.getApiId();
        setResponseEnvelope(response, null, apiId, script.getVersion());
        return new ResponseEntity<Response>(response, status);
    }
    
    protected ResponseEntity<Response> getExceptionResponseEntity(Exception e, String apiId) {
        HttpStatus status = getHttpStatus(e);
        Response response = getErrorResponse(e);
        setResponseEnvelope(response, API_ID_PREFIX, apiId, null);
        return new ResponseEntity<Response>(response, status);
    }

    protected HttpStatus getStatus(int statusCode) {
        HttpStatus status = null;
        try {
            status = HttpStatus.valueOf(statusCode);
        } catch (Exception e) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return status;
    }

    protected void setResponseEnvelope(Response response, String prefix, String apiId, String version) {
        if (null != response) {
            if (StringUtils.isBlank(prefix))
                response.setId(apiId);
            else
                response.setId(prefix + "." + apiId);
            response.setVer(StringUtils.isBlank(version) ? API_VERSION : version);
            response.setTs(getResponseTimestamp());
            ResponseParams params = response.getParams();
            if (null == params)
                params = new ResponseParams();
            params.setResmsgid(getUUID());
            if (StringUtils.equalsIgnoreCase(ResponseParams.StatusType.successful.name(), params.getStatus())) {
                params.setErr(null);
                params.setErrmsg(null);
            }
            response.setParams(params);
        }
    }

    protected String getResponseTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'XXX");
        return sdf.format(new Date());
    }

    protected String getUUID() {
        UUID uid = UUID.randomUUID();
        return uid.toString();
    }
}

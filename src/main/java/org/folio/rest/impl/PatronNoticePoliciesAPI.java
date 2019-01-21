package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.PatronNoticePolicy;
import org.folio.rest.jaxrs.resource.PatronNoticePolicyStorage;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.ValidationHelper;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class PatronNoticePoliciesAPI implements PatronNoticePolicyStorage {

  private static final Logger logger = LoggerFactory.getLogger(PatronNoticePoliciesAPI.class);

  private static final String PATRON_NOTICE_POLICY_TABLE = "patron_notice_policy";
  public static final String STATUS_CODE_DUPLICATE_NAME = "duplicate.name";

  @Override
  public void postPatronNoticePolicyStoragePatronNoticePolicies(
    PatronNoticePolicy entity,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    vertxContext.runOnContext(v -> {
      try {
        String tenantId = okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT);
        PostgresClient pgClient = PostgresClient.getInstance(vertxContext.owner(), tenantId);

        if (entity.getId() == null) {
          entity.setId(UUID.randomUUID().toString());
        }

        pgClient.save(PATRON_NOTICE_POLICY_TABLE, entity.getId(), entity, save -> {
          if (save.failed()) {
            logger.error(save.cause());
            if (ValidationHelper.isDuplicate(save.cause().getMessage())) {
              asyncResultHandler.handle(Future.succeededFuture(
                PostPatronNoticePolicyStoragePatronNoticePoliciesResponse
                  .respond422WithApplicationJson(createNotUniqueNameErrors(entity.getName()))));
            }
            asyncResultHandler.handle(Future.succeededFuture(
              PostPatronNoticePolicyStoragePatronNoticePoliciesResponse.respond500WithTextPlain(save.cause())));
            return;
          }
          asyncResultHandler.handle(Future.succeededFuture(
            PostPatronNoticePolicyStoragePatronNoticePoliciesResponse.respond201WithApplicationJson(entity)));
        });
      } catch (Exception e) {
        logger.error(e);
        asyncResultHandler.handle(Future.succeededFuture(
          PostPatronNoticePolicyStoragePatronNoticePoliciesResponse.respond500WithTextPlain(e)));
      }
    });
  }

  @Override
  public void putPatronNoticePolicyStoragePatronNoticePoliciesByPatronNoticePolicyId(
    String patronNoticePolicyId,
    PatronNoticePolicy entity,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    vertxContext.runOnContext(v -> {
      try {
        String tenantId = okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT);
        PostgresClient pgClient = PostgresClient.getInstance(vertxContext.owner(), tenantId);

        pgClient.update(PATRON_NOTICE_POLICY_TABLE, entity, patronNoticePolicyId, update -> {
          if (update.failed()) {
            logger.error(update.cause());
            if (ValidationHelper.isDuplicate(update.cause().getMessage())) {
              asyncResultHandler.handle(Future.succeededFuture(
                PutPatronNoticePolicyStoragePatronNoticePoliciesByPatronNoticePolicyIdResponse
                  .respond422WithApplicationJson(createNotUniqueNameErrors(entity.getName()))));
              return;
            }
            asyncResultHandler.handle(Future.succeededFuture(
              PutPatronNoticePolicyStoragePatronNoticePoliciesByPatronNoticePolicyIdResponse
                .respond500WithTextPlain(update.cause())));
            return;
          }

          if (update.result().getUpdated() == 0) {
            asyncResultHandler.handle(Future.succeededFuture(
              PutPatronNoticePolicyStoragePatronNoticePoliciesByPatronNoticePolicyIdResponse
                .respond404WithTextPlain("Not found")));
            return;
          }
          asyncResultHandler.handle(Future.succeededFuture(
            PutPatronNoticePolicyStoragePatronNoticePoliciesByPatronNoticePolicyIdResponse.respond204()));
        });
      } catch (Exception e) {
        logger.error(e);
        asyncResultHandler.handle(Future.succeededFuture(
          PutPatronNoticePolicyStoragePatronNoticePoliciesByPatronNoticePolicyIdResponse.respond500WithTextPlain(e)));
      }
    });
  }

  private Errors createNotUniqueNameErrors(String name) {
    Error error = new Error();
    error.setMessage(String.format("'%s' name in not unique", name));
    error.setCode(STATUS_CODE_DUPLICATE_NAME);
    return new Errors().withErrors(Collections.singletonList(error));
  }
}
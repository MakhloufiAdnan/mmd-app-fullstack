package com.openclassrooms.mdd_api.common.web;

/**
 * Codes d'erreur standardisés (contrat API).
 *
 * Bonnes pratiques prod :
 * - centraliser les "literals" (évite les typos et facilite l'évolution)
 * - garder des codes stables côté client (front/Postman/tests)
 */
public final class ApiErrorCodes {

    private ApiErrorCodes() {}

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String CONFLICT = "CONFLICT";
    public static final String INTERNAL = "INTERNAL";
}

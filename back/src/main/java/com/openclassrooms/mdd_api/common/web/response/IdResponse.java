package com.openclassrooms.mdd_api.common.web.response;

/**
 * Réponse standard utilisée lorsqu'un point de terminaison ne renvoie qu'un identifiant.
 * Garantit la cohérence des réponses de l'API entre les différentes fonctionnalités.
 */
public record IdResponse(
        Long id
){}

package com.openclassrooms.mdd_api.topic.bootstrap;

import com.openclassrooms.mdd_api.topic.entity.Topic;
import com.openclassrooms.mdd_api.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Order(0)
@RequiredArgsConstructor
public class TopicSeeder implements ApplicationRunner {

    private final TopicRepository topicRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (topicRepository.count() > 0) {
            return;
        }

        topicRepository.saveAll(List.of(
                new Topic(
                        "Angular",
                        "Framework front moderne pour construire des interfaces réactives. " +
                                "Idéal pour structurer une SPA avec composants, routing et services. " +
                                "Parfait pour un MVP solide et maintenable."
                ),
                new Topic(
                        "Spring Boot",
                        "Framework back Java pour créer des APIs REST rapidement. " +
                                "Sécurité, validation, JPA et configuration simplifiées. " +
                                "Un standard pour des services robustes."
                ),
                new Topic(
                        "Clean Code",
                        "Écrire du code lisible, testable et facile à faire évoluer. " +
                                "Nommage, petites fonctions, responsabilité unique. " +
                                "Moins de bugs, plus de sérénité en équipe."
                ),
                new Topic(
                        "DevOps",
                        "Automatiser la livraison : build, tests, déploiement, monitoring. " +
                                "CI/CD, conteneurs, logs et observabilité. " +
                                "Gagner en vitesse sans sacrifier la qualité."
                ),
                new Topic(
                        "Architecture",
                        "Structurer une application pour qu’elle reste maintenable dans le temps. " +
                                "Découpage par features, SOLID, patterns et frontières claires. " +
                                "Moins de couplage, plus d’évolutivité."
                ),
                new Topic(
                        "Testing",
                        "Sécuriser le code avec des tests unitaires et d’intégration. " +
                                "Cas limites, mocks raisonnables, tests REST contrôleurs. " +
                                "Un filet de sécurité pour livrer sereinement."
                ),
                new Topic(
                        "Sécurité",
                        "Protéger l’application : auth, CSRF, validation, contrôle d’accès. " +
                                "Ne jamais faire confiance au front, erreurs cohérentes. " +
                                "Réduire les risques sans alourdir le MVP."
                ),
                new Topic(
                        "Cloud",
                        "Déployer et exploiter : environnements, logs, métriques, scalabilité. " +
                                "Conteneurs, config, bonnes pratiques d’exploitation. " +
                                "Aller vers la prod de façon pragmatique."
                )
        ));
    }
}

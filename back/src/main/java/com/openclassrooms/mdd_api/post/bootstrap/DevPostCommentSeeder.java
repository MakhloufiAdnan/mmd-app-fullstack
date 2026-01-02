package com.openclassrooms.mdd_api.post.bootstrap;

import com.openclassrooms.mdd_api.comment.entity.Comment;
import com.openclassrooms.mdd_api.comment.repository.CommentRepository;
import com.openclassrooms.mdd_api.post.entity.Post;
import com.openclassrooms.mdd_api.post.repository.PostRepository;
import com.openclassrooms.mdd_api.topic.entity.Topic;
import com.openclassrooms.mdd_api.topic.repository.TopicRepository;
import com.openclassrooms.mdd_api.user.entity.User;
import com.openclassrooms.mdd_api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DevPostCommentSeeder implements ApplicationRunner {

    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 1) On évite les doublons : si déjà des posts => on ne reseed pas
        if (postRepository.count() > 0) {
            return;
        }

        // 2) Il faut des topics, sinon seed impossible
        List<Topic> topics = topicRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        if (topics.isEmpty()) {
            return;
        }

        // 3) Créer 2 users "dev" si besoin (auteurs/comm)
        User devUser = getOrCreateUser("user@mail.com", "devUser", "P@ssw0rd!");
        User otherUser = getOrCreateUser("other@mail.com", "otherUser", "P@ssw0rd!");

        // 4) Prendre jusqu’à 3 topics (sans supposer les IDs)
        Topic t1 = topics.get(0);
        Topic t2 = topics.size() > 1 ? topics.get(1) : t1;
        Topic t3 = topics.size() > 2 ? topics.get(2) : t1;

        String seeds = "[Seed] ";

        // 5) Créer quelques posts (titres uniques)
        Post p1 = postRepository.save(new Post(
                seeds + t1.getName() + " — Les fondamentaux",
                "Contenu de démonstration : bases, objectifs, premières étapes.",
                t1,
                devUser
        ));

        Post p2 = postRepository.save(new Post(
                seeds + t2.getName() + " — Astuces pratiques",
                "Contenu de démonstration : astuces, erreurs fréquentes, bonnes pratiques.",
                t2,
                devUser
        ));

        Post p3 = postRepository.save(new Post(
                seeds + t3.getName() + " — Aller plus loin",
                "Contenu de démonstration : pistes avancées, ressources, exercices.",
                t3,
                otherUser
        ));

        // 6) Ajouter quelques comments (non récursifs)
        commentRepository.save(new Comment("Super clair, merci !", p1, otherUser));
        commentRepository.save(new Comment("Je confirme, bon résumé 👌", p1, devUser));

        commentRepository.save(new Comment("Intéressant, tu as une ressource à conseiller ?", p2, otherUser));

        commentRepository.save(new Comment("Top, je vais tester ça.", p3, devUser));
        commentRepository.save(new Comment("Ça m’a débloqué, merci.", p3, otherUser));
    }

    private User getOrCreateUser(String email, String username, String rawPassword) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.save(
                        new User(email, username, passwordEncoder.encode(rawPassword))
                ));
    }
}

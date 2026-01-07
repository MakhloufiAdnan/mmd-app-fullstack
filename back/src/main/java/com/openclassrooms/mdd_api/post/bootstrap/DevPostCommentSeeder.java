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

        // 3) Utilisateurs (3 auteurs)
        String rawPassword = "Aa1!aaaa";
        User bob = getOrCreateUser("bob_marley@example.com", "bob_marley", rawPassword);
        User tom = getOrCreateUser("tom_soyer@example.com", "tom_soyer", rawPassword);
        User ben = getOrCreateUser("ben_jerry@example.com", "ben_jerry", rawPassword);

        List<User> authors = List.of(bob, tom, ben);

        // 4) 4 posts par topic
        String seedPrefix = "[Seed] ";

        for (Topic topic : topics) {
            for (int i = 1; i <= 4; i++) {
                User author = authors.get((i - 1) % authors.size());

                Post post = postRepository.save(new Post(
                        seedPrefix + topic.getName() + " — Article " + i,
                        buildSeedContent(topic.getName(), i),
                        topic,
                        author
                ));

                // 5) 1 commentaire uniquement sur l'article 1 de chaque topic
                if (i == 1) {
                    User commenter = (author.getId().equals(bob.getId())) ? tom : bob;
                    commentRepository.save(new Comment(
                            "Merci pour ce partage ! J'aime bien l'angle \"MVP\".",
                            post,
                            commenter
                    ));
                }
            }
        }
    }

    private String buildSeedContent(String topicName, int articleIndex) {
        // Multiligne pour tester le line-clamp (feed = 5 lignes) + rendu detail.
        return "Article de démonstration sur " + topicName + " (n°" + articleIndex + ").\n"
                + "On aborde une idée clé, avec un exemple simple et concret.\n"
                + "Ensuite on détaille un piège fréquent et comment l'éviter.\n"
                + "On ajoute une bonne pratique applicable dès aujourd'hui.\n"
                + "Enfin, on propose une petite checklist de vérification.\n"
                + "Conclusion : garde ça simple, puis itère.";
    }

    private User getOrCreateUser(String email, String username, String rawPassword) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.save(
                        new User(email, username, passwordEncoder.encode(rawPassword))
                ));
    }
}

package com.securevote.backend.config;

import com.securevote.backend.entity.*;
import com.securevote.backend.repository.CandidateRepository;
import com.securevote.backend.repository.ElectionRepository;
import com.securevote.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seed(UserRepository userRepository,
                           ElectionRepository electionRepository,
                           CandidateRepository candidateRepository,
                           PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setEmail("admin@securevote.local");
                admin.setPassword(passwordEncoder.encode("Admin@123"));
                admin.setRole(Role.ADMIN);
                admin.setDateOfBirth(LocalDate.of(1990, 1, 1));
                userRepository.save(admin);
            }

            if (userRepository.findByUsername("voter1").isEmpty()) {
                User voter = new User();
                voter.setUsername("voter1");
                voter.setEmail("voter1@securevote.local");
                voter.setPassword(passwordEncoder.encode("Voter@123"));
                voter.setRole(Role.USER);
                voter.setDateOfBirth(LocalDate.of(1998, 6, 15));
                userRepository.save(voter);
            }

            if (electionRepository.count() == 0) {
                Election election = new Election();
                election.setTitle("National Digital Election 2026");
                election.setType(ElectionType.LOK_SABHA);
                election.setRegion("India");
                election.setDescription("Demo election seeded on startup");
                election.setStatus(ElectionStatus.ACTIVE);
                election.setStartDate(Instant.now().minusSeconds(3600));
                election.setEndDate(Instant.now().plusSeconds(86400));
                election = electionRepository.save(election);

                Candidate c1 = new Candidate();
                c1.setName("Candidate A");
                c1.setParty("Reform Front");
                c1.setRegion("India");
                c1.setManifesto("Transparency and secure governance");
                c1.setElection(election);

                Candidate c2 = new Candidate();
                c2.setName("Candidate B");
                c2.setParty("People's Alliance");
                c2.setRegion("India");
                c2.setManifesto("Digital growth and public innovation");
                c2.setElection(election);

                candidateRepository.save(c1);
                candidateRepository.save(c2);
            }

            Election indiaElection = electionRepository.findAll().stream()
                    .filter(e -> "India".equalsIgnoreCase(e.getRegion()))
                    .findFirst()
                    .orElseGet(() -> {
                        Election election = new Election();
                        election.setTitle("India General Election Showcase");
                        election.setType(ElectionType.LOK_SABHA);
                        election.setRegion("India");
                        election.setDescription("Seeded election for Indian candidate search demos");
                        election.setStatus(ElectionStatus.ACTIVE);
                        election.setStartDate(Instant.now().minusSeconds(7200));
                        election.setEndDate(Instant.now().plusSeconds(604800));
                        return electionRepository.save(election);
                    });

            List<Candidate> existingCandidates = candidateRepository.findByElectionId(indiaElection.getId());
            Set<String> existingNames = new HashSet<>();
            existingCandidates.forEach(c -> existingNames.add(c.getName().toLowerCase()));

            List<CandidateSeed> indianCandidates = List.of(
                    new CandidateSeed("Aarav Sharma", "Jan Pragati Party", "Delhi, India", "Digital governance and transparent welfare"),
                    new CandidateSeed("Saanvi Iyer", "Lok Seva Dal", "Chennai, Tamil Nadu, India", "Women-led public safety and education"),
                    new CandidateSeed("Vihaan Kulkarni", "Bharat Vikas Manch", "Pune, Maharashtra, India", "Skilled youth employment and startup grants"),
                    new CandidateSeed("Ananya Reddy", "Nagrik Nyay Front", "Hyderabad, Telangana, India", "Urban mobility and clean lakes mission"),
                    new CandidateSeed("Aditya Singh", "Samvidhan Reform Bloc", "Lucknow, Uttar Pradesh, India", "Judicial access and district digitization"),
                    new CandidateSeed("Ira Das", "Green Bharat Collective", "Kolkata, West Bengal, India", "Climate resilience and flood planning"),
                    new CandidateSeed("Krishna Menon", "Federal Unity Party", "Kochi, Kerala, India", "Healthcare modernization and coastal economy"),
                    new CandidateSeed("Meera Bhatia", "Citizen First Alliance", "Jaipur, Rajasthan, India", "Safe cities and rural road connectivity"),
                    new CandidateSeed("Rohan Patel", "People's Development Congress", "Ahmedabad, Gujarat, India", "Manufacturing growth and MSME support"),
                    new CandidateSeed("Nisha Yadav", "Public Accountability Party", "Bhopal, Madhya Pradesh, India", "Anti-corruption and grievance redressal"),
                    new CandidateSeed("Kabir Choudhary", "National Progress Front", "Bengaluru, Karnataka, India", "AI jobs mission and civic tech"),
                    new CandidateSeed("Tara Mukherjee", "Inclusive India Platform", "Patna, Bihar, India", "Education quality and women entrepreneurship")
            );

            for (CandidateSeed seed : indianCandidates) {
                if (existingNames.contains(seed.name().toLowerCase())) {
                    continue;
                }

                Candidate candidate = new Candidate();
                candidate.setName(seed.name());
                candidate.setParty(seed.party());
                candidate.setRegion(seed.region());
                candidate.setManifesto(seed.manifesto());
                candidate.setElection(indiaElection);
                candidateRepository.save(candidate);
            }

                seedRegionalElectionIfMissing(electionRepository, candidateRepository,
                    "Maharashtra Civic Pulse 2026",
                    ElectionType.STATE,
                    "Maharashtra",
                    "Regional governance election demo for Maharashtra users",
                    List.of(
                        new CandidateSeed("Neel Deshmukh", "Maha Jan Front", "Mumbai, Maharashtra", "Public transport and local jobs"),
                        new CandidateSeed("Ishita Patil", "Citizen Reform Bloc", "Pune, Maharashtra", "Water security and smart wards")
                    ));

                seedRegionalElectionIfMissing(electionRepository, candidateRepository,
                    "Karnataka Digital Governance Poll",
                    ElectionType.STATE,
                    "Karnataka",
                    "Regional governance election demo for Karnataka users",
                    List.of(
                        new CandidateSeed("Rahul Hegde", "Namma Karnataka Party", "Bengaluru, Karnataka", "Tech-enabled civic services"),
                        new CandidateSeed("Anika Rao", "Jan Hit Sena", "Mysuru, Karnataka", "Women safety and infrastructure")
                    ));

                seedRegionalElectionIfMissing(electionRepository, candidateRepository,
                    "Delhi Public Services Election",
                    ElectionType.LOCAL,
                    "Delhi",
                    "City-level election demo for Delhi users",
                    List.of(
                        new CandidateSeed("Kabir Malhotra", "Delhi Civic Alliance", "New Delhi, Delhi", "Pollution control and mobility"),
                        new CandidateSeed("Rhea Anand", "Metro Progress Party", "Dwarka, Delhi", "Health centers and school upgrades")
                    ));
        };
    }

            private void seedRegionalElectionIfMissing(ElectionRepository electionRepository,
                                CandidateRepository candidateRepository,
                                String title,
                                ElectionType type,
                                String region,
                                String description,
                                List<CandidateSeed> seeds) {
            Election election = electionRepository.findAll().stream()
                .filter(e -> title.equalsIgnoreCase(e.getTitle()))
                .findFirst()
                .orElseGet(() -> {
                    Election created = new Election();
                    created.setTitle(title);
                    created.setType(type);
                    created.setRegion(region);
                    created.setDescription(description);
                    created.setStatus(ElectionStatus.ACTIVE);
                    created.setStartDate(Instant.now().minusSeconds(3600));
                    created.setEndDate(Instant.now().plusSeconds(604800));
                    return electionRepository.save(created);
                });

            Set<String> existing = candidateRepository.findByElectionId(election.getId())
                .stream()
                .map(c -> c.getName().toLowerCase())
                .collect(java.util.stream.Collectors.toSet());

            for (CandidateSeed seed : seeds) {
                if (existing.contains(seed.name().toLowerCase())) {
                continue;
                }
                Candidate candidate = new Candidate();
                candidate.setName(seed.name());
                candidate.setParty(seed.party());
                candidate.setRegion(seed.region());
                candidate.setManifesto(seed.manifesto());
                candidate.setElection(election);
                candidateRepository.save(candidate);
            }
            }

    private record CandidateSeed(String name, String party, String region, String manifesto) {
    }
}

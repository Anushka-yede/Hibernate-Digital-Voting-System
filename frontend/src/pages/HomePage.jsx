import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';

const reveal = {
  hidden: { opacity: 0, y: 28 },
  show: { opacity: 1, y: 0 }
};

export default function HomePage() {
  return (
    <>
      <section className="hero">
        <div className="container hero-grid">
          <motion.div
            className="hero-text"
            variants={reveal}
            initial="hidden"
            animate="show"
            transition={{ duration: 0.5 }}
          >
            <p className="eyebrow">Transparent. Trusted. Tamper-Evident.</p>
            <h1>India-ready digital voting with blockchain trust rails</h1>
            <p>
              Build citizen trust with one-vote guarantees, role-based dashboards,
              blockchain-backed receipts, and live AI fraud intelligence for election teams.
            </p>
            <div className="hero-actions">
              <Link to="/register" className="primary-btn">Start as Voter</Link>
              <Link to="/login" className="secondary-btn">Sign In</Link>
              <Link to="/candidates" className="secondary-btn">Explore Candidates</Link>
            </div>
            <div className="hero-kpis">
              <div>
                <span>100%</span>
                <p>single-vote integrity</p>
              </div>
              <div>
                <span>AI</span>
                <p>continuous anomaly scans</p>
              </div>
              <div>
                <span>Audit</span>
                <p>on-demand election verification</p>
              </div>
            </div>
          </motion.div>
          <motion.div
            className="hero-card"
            variants={reveal}
            initial="hidden"
            animate="show"
            transition={{ duration: 0.55, delay: 0.12 }}
          >
            <h3>What makes this stack resilient?</h3>
            <ul>
              <li>JWT role-based auth for ADMIN and USER panels</li>
              <li>Election windows enforced by backend state checks</li>
              <li>Blockchain anchors for tamper-evident vote proof</li>
              <li>AI risk scoring surfaced instantly to admins</li>
            </ul>
            <div className="hero-card-foot">
              <span>Trusted workflow</span>
              <strong>Register -> Vote -> Verify</strong>
            </div>
          </motion.div>
        </div>
        <div className="hero-orb hero-orb-1" />
        <div className="hero-orb hero-orb-2" />
      </section>

      <section className="container section-block">
        <motion.h2
          className="section-head"
          variants={reveal}
          initial="hidden"
          whileInView="show"
          viewport={{ once: true, amount: 0.4 }}
          transition={{ duration: 0.45 }}
        >
          How voting works
        </motion.h2>
        <div className="story-grid">
          {[
            ['1. Register', 'Users onboard with age validation and secure account creation.'],
            ['2. Enter Election', 'Active elections are filtered by schedule, type, and region.'],
            ['3. Cast Vote', 'A single candidate is selected and locked with anti-duplicate checks.'],
            ['4. Verify Trail', 'Admins review results, blockchain audit traces, and AI alerts.']
          ].map(([title, desc], idx) => (
            <motion.article
              key={title}
              className="panel story-card"
              variants={reveal}
              initial="hidden"
              whileInView="show"
              viewport={{ once: true, amount: 0.35 }}
              transition={{ duration: 0.35, delay: idx * 0.07 }}
            >
              <h3>{title}</h3>
              <p>{desc}</p>
            </motion.article>
          ))}
        </div>
      </section>

      <section className="container section-block">
        <motion.h2
          className="section-head"
          variants={reveal}
          initial="hidden"
          whileInView="show"
          viewport={{ once: true, amount: 0.4 }}
          transition={{ duration: 0.45 }}
        >
          Built for election operators and citizens
        </motion.h2>
        <div className="feature-grid">
          <article className="panel">
            <h3>Admin command center</h3>
            <p>Create elections, manage candidates area-wise, monitor AI anomalies, and inspect audit logs.</p>
          </article>
          <article className="panel">
            <h3>Voter-first dashboard</h3>
            <p>Quickly discover active and upcoming elections, then vote with immediate confirmation feedback.</p>
          </article>
          <article className="panel">
            <h3>Candidate discovery</h3>
            <p>Search by name, party, and region to compare manifestos before casting your ballot.</p>
          </article>
        </div>
      </section>
    </>
  );
}

import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { electionApi } from '../services/api';
import BackHomeButton from '../components/BackHomeButton';
import Modal from '../components/Modal';
import SkeletonGrid from '../components/SkeletonGrid';
import { useAlert } from '../hooks/useAlert';
import { subscribeRealtime } from '../services/realtime';
import { getSession } from '../services/session';

export default function UserDashboard() {
  const [activeElections, setActiveElections] = useState([]);
  const [upcomingElections, setUpcomingElections] = useState([]);
  const [allElections, setAllElections] = useState([]);
  const [loading, setLoading] = useState(false);
  const [filters, setFilters] = useState({ title: '', region: '' });
  const [voteStatus, setVoteStatus] = useState({});
  const [voteIntent, setVoteIntent] = useState(null);
  const [voteSubmitting, setVoteSubmitting] = useState(false);
  const { pushAlert } = useAlert();

  const load = async () => {
    try {
      setLoading(true);
      const [activeResp, upcomingResp] = await Promise.all([
        electionApi.listActive({ page: 0, size: 50 }),
        electionApi.listUpcoming({ page: 0, size: 50 })
      ]);

      const allResp = await electionApi.listActive({ page: 0, size: 100, scope: 'all' });

      const activeList = activeResp.data.content || [];
      const upcomingList = upcomingResp.data.content || [];
      const allList = allResp.data.content || [];

      setActiveElections(activeList);
      setUpcomingElections(upcomingList);
      setAllElections(allList);

      const statusResponses = await Promise.all(
        activeList.map(async (election) => {
          const statusResp = await electionApi.voteStatus(election.id);
          return [election.id, statusResp.data?.hasVoted === true];
        })
      );
      setVoteStatus(Object.fromEntries(statusResponses));
    } catch (err) {
      pushAlert(err.response?.data?.message || 'Failed to load elections', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();

    const username = getSession()?.user?.username;
    const unsubUser = username
      ? subscribeRealtime(`/topic/users/${username}/notifications`, (payload) => {
        if (payload?.type === 'VOTE_SUCCESS') {
          pushAlert(`Vote recorded. Tx Hash: ${payload.blockchainTxHash}`, 'success');
        }
      })
      : () => {};

    const unsubLifecycle = subscribeRealtime('/topic/elections/lifecycle', (event) => {
      pushAlert(`Election update: ${event.title} is ${event.status}`, 'info');
      load();
    });

    return () => {
      unsubUser();
      unsubLifecycle();
    };
  }, []);

  const vote = async (electionId, candidateId) => {
    try {
      setVoteSubmitting(true);
      const { data } = await electionApi.vote({ electionId, candidateId });
      pushAlert(`Vote accepted. Blockchain tx: ${data.blockchainTxHash}`, 'success');
      await load();
    } catch (err) {
      pushAlert(err.response?.data?.message || 'Vote rejected', 'error');
    } finally {
      setVoteSubmitting(false);
      setVoteIntent(null);
    }
  };

  const filteredActive = activeElections.filter((election) => {
    const titleMatch = election.title.toLowerCase().includes(filters.title.toLowerCase());
    const regionMatch = election.region.toLowerCase().includes(filters.region.toLowerCase());
    return titleMatch && regionMatch;
  });

  const filteredUpcoming = upcomingElections.filter((election) => {
    const titleMatch = election.title.toLowerCase().includes(filters.title.toLowerCase());
    const regionMatch = election.region.toLowerCase().includes(filters.region.toLowerCase());
    return titleMatch && regionMatch;
  });

  return (
    <div className="container page-space">
      <h2 className="page-title">User Dashboard</h2>
      <div className="panel filter-bar">
        <input
          placeholder="Search election title"
          value={filters.title}
          onChange={(e) => setFilters({ ...filters, title: e.target.value })}
        />
        <input
          placeholder="Filter by region"
          value={filters.region}
          onChange={(e) => setFilters({ ...filters, region: e.target.value })}
        />
        <button className="secondary-btn" onClick={load}>Refresh</button>
        <BackHomeButton />
      </div>

      {loading ? <SkeletonGrid count={4} /> : (
        <>
          <h3>Active Elections</h3>
          <div className="card-grid section-gap">
            {filteredActive.length === 0 && <div className="panel empty-state">No active elections available right now.</div>}
            {filteredActive.map((election, idx) => (
              <motion.article
                className="panel"
                key={election.id}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.25, delay: idx * 0.03 }}
              >
                <h3>{election.title}</h3>
                <p className="meta-line">{election.type} | {election.region}</p>
                <p>{election.description}</p>
                <p className={voteStatus[election.id] ? 'status-pill status-success' : 'status-pill status-pending'}>
                  {voteStatus[election.id] ? 'Already Voted' : 'Not Voted'}
                </p>
                <div className="candidate-list">
                  {election.candidates?.map((candidate) => (
                    <button
                      key={candidate.id}
                      className="vote-btn"
                      onClick={() => setVoteIntent({
                        electionId: election.id,
                        electionTitle: election.title,
                        candidateId: candidate.id,
                        candidateName: candidate.name
                      })}
                      disabled={voteStatus[election.id]}
                    >
                      Vote {candidate.name}
                    </button>
                  ))}
                </div>
              </motion.article>
            ))}
          </div>

          <h3 className="section-title">Upcoming Elections</h3>
          <div className="card-grid">
            {filteredUpcoming.length === 0 && <div className="panel empty-state">No upcoming elections found.</div>}
            {filteredUpcoming.map((election, idx) => (
              <motion.article
                className="panel"
                key={election.id}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.25, delay: idx * 0.03 }}
              >
                <h3>{election.title}</h3>
                <p className="meta-line">{election.type} | {election.region}</p>
                <p>{election.description}</p>
                <p className="status-pill status-pending">Upcoming</p>
                <p className="meta-line">Starts: {new Date(election.startDate).toLocaleString()}</p>
              </motion.article>
            ))}
          </div>

          <h3 className="section-title">All Elections (Admin Created Feed)</h3>
          <div className="card-grid">
            {allElections.length === 0 && <div className="panel empty-state">No elections found yet.</div>}
            {allElections.map((election, idx) => (
              <motion.article
                className="panel"
                key={`all-${election.id}`}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.25, delay: idx * 0.02 }}
              >
                <h3>{election.title}</h3>
                <p className="meta-line">{election.type} | {election.region}</p>
                <p>{election.description}</p>
                <p className={election.status === 'ACTIVE' ? 'status-pill status-success' : 'status-pill status-pending'}>
                  {election.status}
                </p>
                <p className="meta-line">Starts: {new Date(election.startDate).toLocaleString()}</p>
                <p className="meta-line">Ends: {new Date(election.endDate).toLocaleString()}</p>
              </motion.article>
            ))}
          </div>
        </>
      )}

      <Modal
        open={Boolean(voteIntent)}
        title="Confirm Vote"
        onClose={() => setVoteIntent(null)}
        footer={(
          <>
            <button className="secondary-btn" type="button" onClick={() => setVoteIntent(null)} disabled={voteSubmitting}>Cancel</button>
            <button
              className="primary-btn"
              type="button"
              disabled={voteSubmitting}
              onClick={() => vote(voteIntent.electionId, voteIntent.candidateId)}
            >
              {voteSubmitting ? 'Submitting...' : 'Confirm Vote'}
            </button>
          </>
        )}
      >
        {voteIntent && (
          <p>
            You are voting for <strong>{voteIntent.candidateName}</strong> in <strong>{voteIntent.electionTitle}</strong>.
            This action cannot be reversed.
          </p>
        )}
      </Modal>
    </div>
  );
}

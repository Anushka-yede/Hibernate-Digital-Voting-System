import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { electionApi } from '../services/api';
import BackHomeButton from '../components/BackHomeButton';
import SkeletonGrid from '../components/SkeletonGrid';
import { useAlert } from '../hooks/useAlert';

export default function CandidateProfilePage() {
  const [filters, setFilters] = useState({ name: '', party: '', region: '' });
  const [regions, setRegions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [candidates, setCandidates] = useState([]);
  const { pushAlert } = useAlert();

  const load = async () => {
    try {
      setLoading(true);
      const { data } = await electionApi.searchCandidates({ ...filters, page: 0, size: 30 });
      setCandidates(data.content || []);
    } catch (err) {
      pushAlert(err.response?.data?.message || 'Failed to fetch candidates', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    electionApi.candidateRegions()
      .then((resp) => setRegions(resp.data || []))
      .catch(() => {});
  }, []);

  return (
    <div className="container page-space">
      <h2 className="page-title">Candidate Profiles</h2>
      <div className="panel filter-bar">
        <input
          placeholder="Search by name"
          value={filters.name}
          onChange={(e) => setFilters({ ...filters, name: e.target.value })}
        />
        <input
          placeholder="Filter by party"
          value={filters.party}
          onChange={(e) => setFilters({ ...filters, party: e.target.value })}
        />
        <select
          value={filters.region}
          onChange={(e) => setFilters({ ...filters, region: e.target.value })}
        >
          <option value="">All Regions</option>
          {regions.map((region) => <option key={region} value={region}>{region}</option>)}
        </select>
        <button className="primary-btn" onClick={load}>Search</button>
        <BackHomeButton />
      </div>

      {regions.length > 0 && (
        <div className="quick-area-row">
          {regions.slice(0, 8).map((region) => (
            <button key={region} type="button" className="secondary-btn compact-btn" onClick={() => setFilters((prev) => ({ ...prev, region }))}>
              {region}
            </button>
          ))}
        </div>
      )}

      {loading ? <SkeletonGrid count={6} /> : (
        <div className="card-grid">
          {candidates.map((candidate, idx) => (
            <motion.article
              className="panel"
              key={candidate.id}
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.3, delay: idx * 0.02 }}
            >
              <h3>{candidate.name}</h3>
              <p><strong>Party:</strong> {candidate.party}</p>
              <p><strong>Region:</strong> {candidate.region}</p>
              <p>{candidate.manifesto || 'Manifesto not provided yet.'}</p>
            </motion.article>
          ))}
        </div>
      )}
    </div>
  );
}

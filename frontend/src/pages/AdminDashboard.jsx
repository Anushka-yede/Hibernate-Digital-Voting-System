import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { electionApi } from '../services/api';
import BackHomeButton from '../components/BackHomeButton';
import SkeletonGrid from '../components/SkeletonGrid';
import { useAlert } from '../hooks/useAlert';
import { subscribeRealtime } from '../services/realtime';

export default function AdminDashboard() {
  const [section, setSection] = useState('elections');
  const [form, setForm] = useState({
    id: null,
    title: '',
    description: '',
    type: 'LOCAL',
    region: '',
    startDate: '',
    endDate: ''
  });
  const [candidate, setCandidate] = useState({ id: null, electionId: '', name: '', party: '', region: '', manifesto: '' });
  const [regionOptions, setRegionOptions] = useState([]);
  const [elections, setElections] = useState([]);
  const [candidates, setCandidates] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [results, setResults] = useState({});
  const [analyticsByElection, setAnalyticsByElection] = useState({});
  const [auditLogs, setAuditLogs] = useState([]);
  const [logFilters, setLogFilters] = useState({ actor: '', action: '' });
  const [loading, setLoading] = useState(false);
  const [bulkCandidateText, setBulkCandidateText] = useState('');
  const [acknowledgedAlertIds, setAcknowledgedAlertIds] = useState([]);
  const { pushAlert } = useAlert();

  const selectedElection = elections.find((election) => String(election.id) === String(candidate.electionId));
  const areaSuggestions = selectedElection
    ? [
        `${selectedElection.region}`,
        `Central ${selectedElection.region}`,
        `North ${selectedElection.region}`,
        `South ${selectedElection.region}`,
        `East ${selectedElection.region}`,
        `West ${selectedElection.region}`
      ]
    : [];

  const load = async () => {
    try {
      setLoading(true);
      const [electionResp, candidateResp, alertsResp] = await Promise.all([
        electionApi.listAdmin({ page: 0, size: 50 }),
        electionApi.searchCandidates({ page: 0, size: 100 }),
        electionApi.alerts({ page: 0, size: 50 })
      ]);
      setElections(electionResp.data.content || []);
      setCandidates(candidateResp.data.content || []);
      setAlerts(alertsResp.data.content || []);
    } catch (err) {
      pushAlert(err.response?.data?.message || 'Failed to load admin dashboard data', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    electionApi.electionRegions()
      .then((resp) => setRegionOptions(resp.data || []))
      .catch(() => {});

    const unsubAlert = subscribeRealtime('/topic/admin/alerts', (event) => {
      setAlerts((prev) => [{
        id: event.alertId,
        userId: event.userId,
        electionId: event.electionId,
        suspicious: event.suspicious,
        score: event.score,
        reason: event.reason,
        createdAt: event.createdAt
      }, ...prev].slice(0, 200));
      pushAlert(`Live AI alert received for election ${event.electionId}.`, 'error');
    });

    const unsubVotes = subscribeRealtime('/topic/admin/votes', (event) => {
      setResults((prev) => ({
        ...prev,
        [event.electionId]: {
          ...(prev[event.electionId] || {}),
          votes: event.candidateVotes
        }
      }));
    });

    const unsubLifecycle = subscribeRealtime('/topic/elections/lifecycle', (event) => {
      pushAlert(`Election ${event.title} is now ${event.status}.`, 'info');
      load();
    });

    return () => {
      unsubAlert();
      unsubVotes();
      unsubLifecycle();
    };
  }, []);

  const loadAuditLogs = async () => {
    try {
      const { data } = await electionApi.auditLogs({
        ...logFilters,
        page: 0,
        size: 50
      });
      setAuditLogs(data.content || []);
    } catch (err) {
      pushAlert(err.response?.data?.message || 'Failed to load audit logs', 'error');
    }
  };

  const saveElection = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        ...form,
        startDate: new Date(form.startDate).toISOString(),
        endDate: new Date(form.endDate).toISOString()
      };

      if (form.id) {
        await electionApi.update(form.id, payload);
        pushAlert('Election updated successfully.', 'success');
      } else {
        await electionApi.create(payload);
        pushAlert('Election created successfully.', 'success');
      }

      setForm({ id: null, title: '', description: '', type: 'LOCAL', region: '', startDate: '', endDate: '' });
      await load();
    } catch (err) {
      pushAlert(err.response?.data?.message || 'Failed to create election', 'error');
    }
  };

  const saveCandidate = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        name: candidate.name,
        party: candidate.party,
        region: candidate.region,
        manifesto: candidate.manifesto
      };

      if (candidate.id) {
        await electionApi.updateCandidate(candidate.id, payload);
        pushAlert('Candidate updated successfully.', 'success');
      } else {
        await electionApi.addCandidate(candidate.electionId, payload);
        pushAlert('Candidate added successfully.', 'success');
      }

      setCandidate({ id: null, electionId: '', name: '', party: '', region: '', manifesto: '' });
      await load();
    } catch (err) {
      pushAlert(err.response?.data?.message || 'Failed to add candidate', 'error');
    }
  };

  const handleBulkAddCandidates = async () => {
    if (!candidate.electionId) {
      pushAlert('Please select an election before bulk adding candidates.', 'error');
      return;
    }

    const rows = bulkCandidateText
      .split('\n')
      .map((line) => line.trim())
      .filter(Boolean);

    if (rows.length === 0) {
      pushAlert('Bulk candidate list is empty.', 'error');
      return;
    }

    const parsedCandidates = rows.map((line) => {
      const [name, party, region, manifesto] = line.split('|').map((part) => part?.trim() || '');
      return { name, party, region, manifesto };
    });

    const invalid = parsedCandidates.find((item) => !item.name || !item.party || !item.region);
    if (invalid) {
      pushAlert('Invalid bulk format. Use: Name | Party | Region/Area | Manifesto', 'error');
      return;
    }

    try {
      await electionApi.addCandidatesBulk(candidate.electionId, { candidates: parsedCandidates });
      pushAlert(`${parsedCandidates.length} candidates added successfully.`, 'success');
      setBulkCandidateText('');
      await load();
    } catch (err) {
      pushAlert(err.response?.data?.message || 'Failed to bulk add candidates', 'error');
    }
  };

  const deleteElection = async (electionId) => {
    try {
      await electionApi.remove(electionId);
      pushAlert('Election deleted.', 'success');
      await load();
    } catch (err) {
      pushAlert(err.response?.data?.message || 'Failed to delete election', 'error');
    }
  };

  const deleteCandidate = async (candidateId) => {
    try {
      await electionApi.removeCandidate(candidateId);
      pushAlert('Candidate deleted.', 'success');
      await load();
    } catch (err) {
      pushAlert(err.response?.data?.message || 'Failed to delete candidate', 'error');
    }
  };

  const fetchResults = async (electionId) => {
    try {
      const [resultResp, auditResp, analyticsResp] = await Promise.all([
        electionApi.results(electionId),
        electionApi.audit(electionId),
        electionApi.analytics(electionId)
      ]);
      setResults((prev) => ({ ...prev, [electionId]: { votes: resultResp.data, audit: auditResp.data } }));
      setAnalyticsByElection((prev) => ({ ...prev, [electionId]: analyticsResp.data }));
      pushAlert('Results loaded.', 'success');
    } catch (err) {
      pushAlert(err.response?.data?.message || 'Failed to load result data', 'error');
    }
  };

  const chartDataForElection = (electionId) => {
    const voteMap = results[electionId]?.votes || {};
    return Object.entries(voteMap).map(([candidateName, votes]) => ({ candidateName, votes }));
  };

  const riskLabel = (score) => {
    if (score >= 0.8) return 'High';
    if (score >= 0.5) return 'Medium';
    return 'Low';
  };

  const acknowledgeAlert = (alertId) => {
    setAcknowledgedAlertIds((prev) => [...new Set([...prev, alertId])]);
  };

  return (
    <div className="container page-space">
      <h2 className="page-title">Admin Dashboard</h2>

      <div className="admin-shell">
        <aside className="panel admin-sidebar">
          <button className={section === 'elections' ? 'primary-btn' : 'secondary-btn'} onClick={() => setSection('elections')}>Manage Elections</button>
          <button className={section === 'candidates' ? 'primary-btn' : 'secondary-btn'} onClick={() => setSection('candidates')}>Manage Candidates</button>
          <button className={section === 'results' ? 'primary-btn' : 'secondary-btn'} onClick={() => setSection('results')}>View Results</button>
          <button className={section === 'alerts' ? 'primary-btn' : 'secondary-btn'} onClick={() => setSection('alerts')}>AI Alerts</button>
          <button className={section === 'audit' ? 'primary-btn' : 'secondary-btn'} onClick={() => { setSection('audit'); loadAuditLogs(); }}>Audit Logs</button>
          <button className="secondary-btn" onClick={load}>Refresh</button>
          <BackHomeButton />
        </aside>

        <section>
          {loading ? <SkeletonGrid count={3} /> : null}

          {section === 'elections' && (
            <motion.div className="panel" initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}>
              <h3>{form.id ? 'Update Election' : 'Create Election'}</h3>
              <form className="form-grid" onSubmit={saveElection}>
                <input placeholder="Title" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required />
                <select value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })}>
                  <option value="LOCAL">LOCAL</option>
                  <option value="STATE">STATE</option>
                  <option value="LOK_SABHA">LOK_SABHA</option>
                  <option value="RAJYA_SABHA">RAJYA_SABHA</option>
                </select>
                <input placeholder="Region" value={form.region} onChange={(e) => setForm({ ...form, region: e.target.value })} required />
                {regionOptions.length > 0 && (
                  <select value={form.region} onChange={(e) => setForm({ ...form, region: e.target.value })}>
                    <option value="">Select Existing Region (optional)</option>
                    {regionOptions.map((region) => <option key={region} value={region}>{region}</option>)}
                  </select>
                )}
                <textarea placeholder="Description" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
                <input type="datetime-local" value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} required />
                <input type="datetime-local" value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} required />
                <button className="primary-btn" type="submit">{form.id ? 'Update' : 'Create'}</button>
              </form>

              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Title</th>
                      <th>Type</th>
                      <th>Region</th>
                      <th>Status</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {elections.map((election) => (
                      <tr key={election.id}>
                        <td>{election.title}</td>
                        <td>{election.type}</td>
                        <td>{election.region}</td>
                        <td>{election.status}</td>
                        <td className="table-actions">
                          <button className="secondary-btn" onClick={() => setForm({
                            id: election.id,
                            title: election.title,
                            description: election.description || '',
                            type: election.type,
                            region: election.region,
                            startDate: new Date(election.startDate).toISOString().slice(0, 16),
                            endDate: new Date(election.endDate).toISOString().slice(0, 16)
                          })}>Edit</button>
                          <button className="ghost-btn" onClick={() => deleteElection(election.id)}>Delete</button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </motion.div>
          )}

          {section === 'candidates' && (
            <motion.div className="panel" initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}>
              <h3>{candidate.id ? 'Update Candidate' : 'Add Candidate'}</h3>
              <form className="form-grid" onSubmit={saveCandidate}>
                <select
                  value={candidate.electionId}
                  onChange={(e) => {
                    const electionId = e.target.value;
                    const election = elections.find((item) => String(item.id) === String(electionId));
                    setCandidate({
                      ...candidate,
                      electionId,
                      region: candidate.id ? candidate.region : (election?.region || '')
                    });
                  }}
                  required
                >
                  <option value="">Select Election</option>
                  {elections.map((election) => <option key={election.id} value={election.id}>{election.title}</option>)}
                </select>
                <input placeholder="Candidate Name" value={candidate.name} onChange={(e) => setCandidate({ ...candidate, name: e.target.value })} required />
                <input placeholder="Party" value={candidate.party} onChange={(e) => setCandidate({ ...candidate, party: e.target.value })} required />
                <input placeholder="Region / Area (e.g., Pune, Maharashtra)" value={candidate.region} onChange={(e) => setCandidate({ ...candidate, region: e.target.value })} required />
                <textarea placeholder="Manifesto" value={candidate.manifesto} onChange={(e) => setCandidate({ ...candidate, manifesto: e.target.value })} />
                <button className="primary-btn" type="submit">{candidate.id ? 'Update' : 'Add Candidate'}</button>
              </form>

              {areaSuggestions.length > 0 && (
                <div className="quick-area-row">
                  {areaSuggestions.map((area) => (
                    <button key={area} type="button" className="secondary-btn compact-btn" onClick={() => setCandidate({ ...candidate, region: area })}>
                      {area}
                    </button>
                  ))}
                  {regionOptions.map((area) => (
                    <button key={`region-${area}`} type="button" className="secondary-btn compact-btn" onClick={() => setCandidate({ ...candidate, region: area })}>
                      {area}
                    </button>
                  ))}
                </div>
              )}

              <div className="panel mini-panel">
                <h4>Bulk Add Candidates (Area-wise)</h4>
                <p>Use one line per candidate in this format: Name | Party | Region/Area | Manifesto</p>
                <textarea
                  value={bulkCandidateText}
                  onChange={(e) => setBulkCandidateText(e.target.value)}
                  placeholder={"Aditi Verma | Jan Pragati Party | Mumbai, Maharashtra | Urban mobility and jobs\nRahul Nair | Public Service Front | Kochi, Kerala | Healthcare modernization"}
                />
                <button type="button" className="primary-btn" onClick={handleBulkAddCandidates}>Add Bulk Candidates</button>
              </div>

              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Name</th>
                      <th>Party</th>
                      <th>Region</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {candidates.map((item) => (
                      <tr key={item.id}>
                        <td>{item.name}</td>
                        <td>{item.party}</td>
                        <td>{item.region}</td>
                        <td className="table-actions">
                          <button className="secondary-btn" onClick={() => setCandidate(item)}>Edit</button>
                          <button className="ghost-btn" onClick={() => deleteCandidate(item.id)}>Delete</button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </motion.div>
          )}

          {section === 'results' && (
            <section className="panel">
              <h3>Results and Audit</h3>
              {elections.map((election) => (
                <div className="result-row" key={election.id}>
                  <div>
                    <strong>{election.title}</strong>
                    <p>{election.description}</p>
                  </div>
                  <button className="secondary-btn" onClick={() => fetchResults(election.id)}>Load Result</button>
                  {results[election.id] && (
                    <div className="result-chart-wrap">
                      <ResponsiveContainer width="100%" height={240}>
                        <BarChart data={chartDataForElection(election.id)}>
                          <CartesianGrid strokeDasharray="3 3" />
                          <XAxis dataKey="candidateName" />
                          <YAxis allowDecimals={false} />
                          <Tooltip />
                          <Bar dataKey="votes" fill="#0f7b6c" radius={[8, 8, 0, 0]} />
                        </BarChart>
                      </ResponsiveContainer>
                      {analyticsByElection[election.id] && (
                        <div className="analytics-grid">
                          <div className="panel mini-panel">
                            <strong>Participation Rate</strong>
                            <p>{analyticsByElection[election.id].participationRate?.toFixed(2)}%</p>
                          </div>
                          <div className="panel mini-panel">
                            <strong>Total Votes</strong>
                            <p>{analyticsByElection[election.id].totalVotes}</p>
                          </div>
                          <div className="panel mini-panel">
                            <strong>Eligible Voters</strong>
                            <p>{analyticsByElection[election.id].totalEligibleVoters}</p>
                          </div>
                        </div>
                      )}
                      {analyticsByElection[election.id]?.regionVotes?.length > 0 && (
                        <ResponsiveContainer width="100%" height={220}>
                          <BarChart data={analyticsByElection[election.id].regionVotes}>
                            <CartesianGrid strokeDasharray="3 3" />
                            <XAxis dataKey="region" />
                            <YAxis allowDecimals={false} />
                            <Tooltip />
                            <Bar dataKey="votes" fill="#e85f33" radius={[8, 8, 0, 0]} />
                          </BarChart>
                        </ResponsiveContainer>
                      )}
                      <details>
                        <summary>Audit details</summary>
                        <pre>{JSON.stringify(results[election.id].audit, null, 2)}</pre>
                      </details>
                    </div>
                  )}
                </div>
              ))}
            </section>
          )}

          {section === 'alerts' && (
            <section className="panel">
              <h3>Vote Attempt Alerts</h3>
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Time</th>
                      <th>User</th>
                      <th>Election</th>
                      <th>Score</th>
                      <th>Reason</th>
                      <th>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {alerts.map((alert) => (
                      <tr key={alert.id} className={acknowledgedAlertIds.includes(alert.id) ? 'alert-row-ack' : ''}>
                        <td>{new Date(alert.createdAt).toLocaleString()}</td>
                        <td>{alert.userId}</td>
                        <td>{alert.electionId}</td>
                        <td>
                          <span className={`risk-pill risk-${riskLabel(alert.score).toLowerCase()}`}>
                            {alert.score} ({riskLabel(alert.score)})
                          </span>
                        </td>
                        <td>{alert.reason}</td>
                        <td>
                          <button
                            type="button"
                            className="secondary-btn compact-btn"
                            onClick={() => acknowledgeAlert(alert.id)}
                            disabled={acknowledgedAlertIds.includes(alert.id)}
                          >
                            {acknowledgedAlertIds.includes(alert.id) ? 'Acknowledged' : 'Acknowledge'}
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          )}

          {section === 'audit' && (
            <section className="panel">
              <h3>Audit Logs</h3>
              <div className="filter-bar">
                <input
                  placeholder="Filter by actor"
                  value={logFilters.actor}
                  onChange={(e) => setLogFilters((prev) => ({ ...prev, actor: e.target.value }))}
                />
                <input
                  placeholder="Filter by action"
                  value={logFilters.action}
                  onChange={(e) => setLogFilters((prev) => ({ ...prev, action: e.target.value }))}
                />
                <button className="secondary-btn" type="button" onClick={loadAuditLogs}>Apply</button>
              </div>
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Time</th>
                      <th>Actor</th>
                      <th>Action</th>
                      <th>Details</th>
                    </tr>
                  </thead>
                  <tbody>
                    {auditLogs.map((log) => (
                      <tr key={log.id}>
                        <td>{new Date(log.createdAt).toLocaleString()}</td>
                        <td>{log.actor}</td>
                        <td>{log.action}</td>
                        <td>{log.details}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          )}
        </section>
      </div>
    </div>
  );
}

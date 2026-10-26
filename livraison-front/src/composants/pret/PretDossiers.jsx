import { api, urlInstanceCockpit } from '../../api.js';
import { euros } from '../../domaine.js';
import {
  LIBELLES_VARIABLES,
  RISQUES,
  etapesPret,
  statutPretLisible,
  tonStatutPret,
} from '../../domainePret.js';
import { Badge, Bouton, Carte, EtatVide } from '../ui.jsx';

function Suivi({ demande, tacheActive }) {
  return (
    <ol className="suivi">
      {etapesPret(demande, tacheActive).map((etape) => (
        <li key={etape.cle} className={`suivi__etape suivi__etape--${etape.etat}`}>
          <span className="suivi__point" aria-hidden="true" />
          <span className="suivi__libelle">{etape.libelle}</span>
        </li>
      ))}
    </ol>
  );
}

function valeurLisible(valeur) {
  if (typeof valeur === 'boolean') return valeur ? 'oui' : 'non';
  if (valeur === '' || valeur === null || valeur === undefined) return '—';
  return String(valeur);
}

/**
 * Les timers BPMN (15 j, 10 j, 30 j) ne peuvent pas etre attendus pendant une
 * demo : le bouton execute le job tout de suite, comme le ferait Cockpit.
 */
function Timers({ demande, executer }) {
  if (demande.timers.length === 0) return null;
  return (
    <div className="timers">
      {demande.timers.map((timer) => (
        <div key={timer.jobId} className="timer">
          <span className="timer__icone" aria-hidden="true">
            ⏱
          </span>
          <span className="timer__texte">
            <strong>{timer.libelle}</strong>
            <span>Échéance : {new Date(timer.echeance).toLocaleString('fr-FR')}</span>
          </span>
          <Bouton
            onClick={() =>
              executer(
                () => api.declencherTimer(timer.jobId),
                (maj) => `${timer.libelle} écoulé — ${maj.id} : ${statutPretLisible(maj.statut)}`,
              )
            }
          >
            ⏩ Faire expirer maintenant
          </Bouton>
        </div>
      ))}
    </div>
  );
}

function Fiche({ demande, tacheActive }) {
  const risque = RISQUES[demande.variables.niveauRisque];
  return (
    <dl className="fiche">
      <div>
        <dt>Emprunteur</dt>
        <dd>{demande.emprunteur}</dd>
      </div>
      <div>
        <dt>Revenus / mois</dt>
        <dd>{euros(demande.revenusMensuels)}</dd>
      </div>
      <div>
        <dt>Prix du bien · apport</dt>
        <dd>
          {euros(demande.valeurBien)} · {euros(demande.apport)}
        </dd>
      </div>
      <div>
        <dt>Niveau de risque</dt>
        <dd>{risque ? <Badge ton={risque.ton}>{risque.libelle}</Badge> : 'Pas encore évalué'}</dd>
      </div>
      <div>
        <dt>Offre</dt>
        <dd>
          {demande.tauxAnnuel != null
            ? `${demande.tauxAnnuel} % — ${euros(demande.mensualite)} / mois`
            : 'Pas encore émise'}
        </dd>
      </div>
      <div>
        <dt>{demande.numeroContrat ? 'Contrat' : demande.motifRefus ? 'Motif du refus' : 'Tâche en attente'}</dt>
        <dd>{demande.numeroContrat ?? demande.motifRefus ?? (tacheActive ? tacheActive.nom : 'Aucune')}</dd>
      </div>
      <div>
        <dt>Instance Camunda</dt>
        <dd>
          {demande.processInstanceId ? (
            <a
              href={urlInstanceCockpit(demande.processInstanceId)}
              target="_blank"
              rel="noreferrer"
              className="lien-cockpit"
            >
              Ouvrir dans Cockpit ↗
            </a>
          ) : (
            '—'
          )}
        </dd>
      </div>
    </dl>
  );
}

function Variables({ variables }) {
  const affichees = Object.keys(LIBELLES_VARIABLES).filter((nom) => nom in variables);
  if (affichees.length === 0) return null;
  return (
    <details className="variables">
      <summary>Variables de processus ({affichees.length})</summary>
      <table className="tableau">
        <tbody>
          {affichees.map((nom) => (
            <tr key={nom}>
              <td>{LIBELLES_VARIABLES[nom]}</td>
              <td>
                <code>{nom}</code>
              </td>
              <td>{valeurLisible(variables[nom])}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </details>
  );
}

export default function PretDossiers({ prets, taches, selection, surSelection, executer }) {
  if (prets.length === 0) {
    return (
      <EtatVide icone="🏠" titre="Aucune demande de prêt">
        Déposez une demande : une instance de ProcessusPretHypothecaire démarre aussitôt.
      </EtatVide>
    );
  }

  const tacheDe = (demandeId) => taches.find((tache) => tache.demandeId === demandeId);
  const recentes = [...prets].reverse();

  return (
    <div className="commandes">
      {recentes.map((demande) => {
        const ouverte = selection === demande.id;
        const tacheActive = tacheDe(demande.id);

        return (
          <Carte key={demande.id} className={`commande ${ouverte ? 'commande--ouverte' : ''}`}>
            <button
              className="commande__entete"
              onClick={() => surSelection(ouverte ? null : demande.id)}
              aria-expanded={ouverte}
            >
              <span className="commande__vignette">🏠</span>

              <span className="commande__identite">
                <strong>{demande.id}</strong>
                <span className="commande__sous-titre commande__sous-titre--brut">
                  {demande.emprunteur} — {demande.dureeMois / 12} ans
                  {demande.timers.length > 0 && <Badge ton="info">⏱ timer</Badge>}
                </span>
              </span>

              <span className="commande__montant">{euros(demande.montantDemande)}</span>

              <Badge ton={tonStatutPret(demande.statut)}>{statutPretLisible(demande.statut)}</Badge>

              <span className="commande__chevron" aria-hidden="true">
                {ouverte ? '▾' : '▸'}
              </span>
            </button>

            {ouverte && (
              <div className="commande__detail">
                <Suivi demande={demande} tacheActive={tacheActive} />
                <Timers demande={demande} executer={executer} />
                <Fiche demande={demande} tacheActive={tacheActive} />
                <Variables variables={demande.variables} />
              </div>
            )}
          </Carte>
        );
      })}
    </div>
  );
}

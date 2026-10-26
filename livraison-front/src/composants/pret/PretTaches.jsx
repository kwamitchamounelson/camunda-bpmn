import { useState } from 'react';
import { api, urlInstanceCockpit } from '../../api.js';
import { euros } from '../../domaine.js';
import { RISQUES, UTILISATEURS_PRET, utilisateurPretSuggere } from '../../domainePret.js';
import { Badge, Bouton, Carte, Champ, EtatVide } from '../ui.jsx';

/** Ordre des colonnes = ordre d'apparition des taches dans le BPMN. */
const COLONNES = [
  { titre: 'Conseiller', cles: ['Task_Completer', 'Task_Acceptation'] },
  { titre: 'Expertise', cles: ['Task_Expertise'] },
  { titre: 'Décision de crédit', cles: ['Task_Analyse', 'Task_Comite'] },
  { titre: 'Notaire', cles: ['Task_Signature'] },
];

/* ---------- Formulaires, un par tache utilisateur du BPMN ---------- */

function FormulaireCompletude({ demande, completer }) {
  const [revenus, setRevenus] = useState(demande?.revenusMensuels || 5500);
  const [valeurBien, setValeurBien] = useState(demande?.valeurBien || 240000);
  const [commentaire, setCommentaire] = useState('');
  const manquantes = demande?.variables.piecesManquantes;

  return (
    <>
      {manquantes && (
        <p className="tache__alerte">
          Pièces manquantes : <strong>{manquantes}</strong>
        </p>
      )}
      <div className="tache__champs">
        <Champ label="Revenus mensuels (€)">
          <input type="number" min="0" value={revenus} onChange={(e) => setRevenus(Number(e.target.value))} />
        </Champ>
        <Champ label="Prix du bien (€)">
          <input type="number" min="0" value={valeurBien} onChange={(e) => setValeurBien(Number(e.target.value))} />
        </Champ>
        <Champ label="Commentaire">
          <input value={commentaire} placeholder="Pièces reçues…" onChange={(e) => setCommentaire(e.target.value)} />
        </Champ>
      </div>
      <div className="tache__actions">
        <Bouton
          variante="principal"
          onClick={() =>
            completer(
              { revenusMensuels: revenus, valeurBien, commentaireCompletude: commentaire },
              'Dossier complété : la complétude est revérifiée',
            )
          }
        >
          Transmettre les pièces
        </Bouton>
      </div>
    </>
  );
}

function FormulaireExpertise({ demande, completer }) {
  const [valeur, setValeur] = useState(demande?.valeurBien ?? 0);
  const [commentaire, setCommentaire] = useState('');

  return (
    <>
      <div className="tache__champs">
        <Champ
          label="Valeur expertisée (€)"
          indice={demande && `Sous ${euros(demande.montantDemande)} (montant emprunté), le risque devient « élevé »`}
        >
          <input type="number" min="1" value={valeur} onChange={(e) => setValeur(Number(e.target.value))} />
        </Champ>
        <Champ label="Commentaire">
          <input value={commentaire} placeholder="État du bien…" onChange={(e) => setCommentaire(e.target.value)} />
        </Champ>
      </div>
      <div className="tache__actions">
        <Bouton
          variante="principal"
          onClick={() =>
            completer(
              { valeurExpertisee: valeur, commentaireExpertise: commentaire },
              'Expertise transmise : le DMN risquePret a été évalué',
            )
          }
        >
          Valider l'expertise
        </Bouton>
      </div>
    </>
  );
}

function FormulaireDecision({ demande, completer }) {
  const [taux, setTaux] = useState('3.5');
  const [refusEnCours, setRefusEnCours] = useState(false);
  const [motif, setMotif] = useState('');
  const variables = demande?.variables ?? {};
  const risque = RISQUES[variables.niveauRisque];

  return (
    <>
      <div className="indicateurs">
        <div>
          <span>Score</span>
          <strong>{variables.scoreCredit ?? '—'}</strong>
        </div>
        <div>
          <span>Endettement</span>
          <strong>{variables.tauxEndettement != null ? `${variables.tauxEndettement} %` : '—'}</strong>
        </div>
        <div>
          <span>Valeur expertisée</span>
          <strong>{variables.valeurExpertisee != null ? euros(variables.valeurExpertisee) : '—'}</strong>
        </div>
        <div>
          <span>Risque</span>
          <strong>{risque ? <Badge ton={risque.ton}>{risque.libelle}</Badge> : '—'}</strong>
        </div>
      </div>

      {refusEnCours ? (
        <div className="tache__refus">
          <input autoFocus placeholder="Motif du refus" value={motif} onChange={(e) => setMotif(e.target.value)} />
          <Bouton
            variante="danger"
            onClick={() => completer({ pretAccorde: false, motifRefus: motif }, 'Prêt refusé')}
          >
            Confirmer le refus
          </Bouton>
          <Bouton onClick={() => setRefusEnCours(false)}>Annuler</Bouton>
        </div>
      ) : (
        <div className="tache__actions">
          <input
            className="tache__taux"
            value={taux}
            aria-label="Taux proposé (%)"
            onChange={(e) => setTaux(e.target.value)}
          />
          <Bouton
            variante="principal"
            onClick={() =>
              completer({ pretAccorde: true, tauxPropose: taux }, `Prêt accordé à ${taux} % : offre émise`)
            }
          >
            Accorder
          </Bouton>
          <Bouton variante="danger" onClick={() => setRefusEnCours(true)}>
            Refuser
          </Bouton>
        </div>
      )}
    </>
  );
}

function FormulaireAcceptation({ demande, completer }) {
  return (
    <>
      {demande?.tauxAnnuel != null && (
        <p className="tache__offre">
          Offre : <strong>{euros(demande.montantDemande)}</strong> sur {demande.dureeMois / 12} ans à{' '}
          <strong>{demande.tauxAnnuel} %</strong>, soit <strong>{euros(demande.mensualite)}</strong> / mois
        </p>
      )}
      <div className="tache__actions">
        <Bouton variante="principal" onClick={() => completer({ offreAcceptee: true }, 'Offre acceptée par le client')}>
          Le client accepte
        </Bouton>
        <Bouton variante="danger" onClick={() => completer({ offreAcceptee: false }, 'Offre déclinée par le client')}>
          Le client décline
        </Bouton>
      </div>
    </>
  );
}

function FormulaireSignature({ completer }) {
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10));
  return (
    <div className="tache__actions">
      <input type="date" value={date} aria-label="Date de signature" onChange={(e) => setDate(e.target.value)} />
      <Bouton
        variante="principal"
        onClick={() => completer({ dateSignature: date }, 'Acte signé : fonds débloqués, prêt octroyé')}
      >
        Acte signé
      </Bouton>
    </div>
  );
}

const FORMULAIRES = {
  Task_Completer: FormulaireCompletude,
  Task_Expertise: FormulaireExpertise,
  Task_Analyse: FormulaireDecision,
  Task_Comite: FormulaireDecision,
  Task_Acceptation: FormulaireAcceptation,
  Task_Signature: FormulaireSignature,
};

/* ---------- Carte de tache ---------- */

function CarteTachePret({ tache, demande, executer }) {
  const [utilisateur, setUtilisateur] = useState(utilisateurPretSuggere(tache.groupesCandidats));
  const Formulaire = FORMULAIRES[tache.definitionKey];

  const completer = (variables, messageSucces) =>
    executer(() => api.completerTachePret(tache.id, variables), messageSucces);

  return (
    <article className="tache">
      <header className="tache__entete">
        <span className="tache__vignette">🏠</span>
        <div>
          <strong>{tache.nom}</strong>
          <p className="tache__sous-titre tache__sous-titre--brut">
            {tache.demandeId}
            {demande && ` — ${demande.emprunteur}`}
          </p>
        </div>
        {demande && <span className="tache__montant">{euros(demande.montantDemande)}</span>}
      </header>

      <div className="tache__meta">
        {tache.groupesCandidats.map((groupe) => (
          <Badge key={groupe} ton="info">
            {groupe}
          </Badge>
        ))}
        {tache.assignee ? (
          <Badge ton="succes">Prise par {tache.assignee}</Badge>
        ) : (
          <Badge ton="neutre">Non attribuée</Badge>
        )}
      </div>

      <div className="tache__attribution">
        {tache.assignee ? (
          <Bouton onClick={() => executer(() => api.libererTachePret(tache.id), 'Tâche libérée')}>Libérer</Bouton>
        ) : (
          <>
            <select value={utilisateur} onChange={(e) => setUtilisateur(e.target.value)}>
              {UTILISATEURS_PRET.map((u) => (
                <option key={u} value={u}>
                  {u}
                </option>
              ))}
            </select>
            <Bouton
              onClick={() =>
                executer(() => api.attribuerTachePret(tache.id, utilisateur), `Tâche attribuée à ${utilisateur}`)
              }
            >
              Prendre en charge
            </Bouton>
          </>
        )}
      </div>

      {Formulaire && <Formulaire demande={demande} completer={completer} />}

      <footer className="tache__pied">
        <a href={urlInstanceCockpit(tache.processInstanceId)} target="_blank" rel="noreferrer" className="lien-cockpit">
          Voir l'instance dans Cockpit ↗
        </a>
      </footer>
    </article>
  );
}

export default function PretTaches({ taches, prets, executer }) {
  if (taches.length === 0) {
    return (
      <EtatVide icone="✅" titre="Aucune tâche de prêt en attente">
        Une instance en délai de réflexion n'a pas de tâche : faites expirer son timer depuis l'onglet Dossiers.
      </EtatVide>
    );
  }

  const demandeDe = (id) => prets.find((demande) => demande.id === id);

  return (
    <div className="back-office">
      {COLONNES.map((colonne) => {
        const filtrees = taches.filter((tache) => colonne.cles.includes(tache.definitionKey));
        return (
          <Carte key={colonne.titre} titre={`${colonne.titre} (${filtrees.length})`}>
            {filtrees.length === 0 ? (
              <p className="texte-doux">Rien en attente.</p>
            ) : (
              filtrees.map((tache) => (
                <CarteTachePret key={tache.id} tache={tache} demande={demandeDe(tache.demandeId)} executer={executer} />
              ))
            )}
          </Carte>
        );
      })}
    </div>
  );
}

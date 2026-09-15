import { useState } from 'react';
import { api, urlInstanceCockpit } from '../api.js';
import { UTILISATEURS, estValidation, euros, utilisateurSuggere, vignette } from '../domaine.js';
import { Badge, Bouton, Carte, EtatVide } from './ui.jsx';

function CarteTache({ tache, commande, executer }) {
  const [utilisateur, setUtilisateur] = useState(utilisateurSuggere(tache.groupesCandidats));
  const [refusEnCours, setRefusEnCours] = useState(false);
  const [motif, setMotif] = useState('');
  const validation = estValidation(tache);

  return (
    <article className="tache">
      <header className="tache__entete">
        <span className="tache__vignette">{vignette(commande?.produit)}</span>
        <div>
          <strong>{tache.nom}</strong>
          <p className="tache__sous-titre">
            {tache.commandeId}
            {commande && ` — ${commande.quantite} × ${commande.produit}, ${commande.client}`}
          </p>
        </div>
        {commande?.montant > 0 && <span className="tache__montant">{euros(commande.montant)}</span>}
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
          <Bouton onClick={() => executer(() => api.libererTache(tache.id), 'Tâche libérée')}>
            Libérer
          </Bouton>
        ) : (
          <>
            <select value={utilisateur} onChange={(e) => setUtilisateur(e.target.value)}>
              {UTILISATEURS.map((u) => (
                <option key={u} value={u}>
                  {u}
                </option>
              ))}
            </select>
            <Bouton
              onClick={() =>
                executer(
                  () => api.attribuerTache(tache.id, utilisateur),
                  `Tâche attribuée à ${utilisateur}`,
                )
              }
            >
              Prendre en charge
            </Bouton>
          </>
        )}
      </div>

      {validation ? (
        refusEnCours ? (
          <div className="tache__refus">
            <input
              autoFocus
              placeholder="Motif du refus"
              value={motif}
              onChange={(e) => setMotif(e.target.value)}
            />
            <Bouton
              variante="danger"
              onClick={() =>
                executer(
                  () => api.deciderTache(tache.id, false, motif),
                  (maj) => `Commande ${maj.id} refusée`,
                )
              }
            >
              Confirmer le refus
            </Bouton>
            <Bouton onClick={() => setRefusEnCours(false)}>Annuler</Bouton>
          </div>
        ) : (
          <div className="tache__actions">
            <Bouton
              variante="principal"
              onClick={() =>
                executer(
                  () => api.deciderTache(tache.id, true, null),
                  (maj) => `Commande ${maj.id} approuvée`,
                )
              }
            >
              Approuver
            </Bouton>
            <Bouton variante="danger" onClick={() => setRefusEnCours(true)}>
              Refuser
            </Bouton>
          </div>
        )
      ) : (
        <div className="tache__actions">
          <Bouton
            variante="principal"
            onClick={() =>
              executer(
                () => api.completerTache(tache.id),
                (maj) => `Commande ${maj.id} préparée : ${maj.statut}`,
              )
            }
          >
            Marquer comme préparée
          </Bouton>
        </div>
      )}

      <footer className="tache__pied">
        <a
          href={urlInstanceCockpit(tache.processInstanceId)}
          target="_blank"
          rel="noreferrer"
          className="lien-cockpit"
        >
          Voir l'instance dans Cockpit ↗
        </a>
      </footer>
    </article>
  );
}

export default function BackOffice({ taches, commandes, executer }) {
  const commandeDe = (id) => commandes.find((commande) => commande.id === id);
  const validations = taches.filter(estValidation);
  const preparations = taches.filter((tache) => !estValidation(tache));

  if (taches.length === 0) {
    return (
      <EtatVide icone="✅" titre="Aucune tâche en attente">
        Les tâches apparaissent ici dès qu'une instance atteint une étape manuelle.
      </EtatVide>
    );
  }

  return (
    <div className="back-office">
      <Carte titre={`Validations (${validations.length})`}>
        {validations.length === 0 ? (
          <p className="texte-doux">Rien à valider.</p>
        ) : (
          validations.map((tache) => (
            <CarteTache
              key={tache.id}
              tache={tache}
              commande={commandeDe(tache.commandeId)}
              executer={executer}
            />
          ))
        )}
      </Carte>

      <Carte titre={`Préparations (${preparations.length})`}>
        {preparations.length === 0 ? (
          <p className="texte-doux">Rien à préparer.</p>
        ) : (
          preparations.map((tache) => (
            <CarteTache
              key={tache.id}
              tache={tache}
              commande={commandeDe(tache.commandeId)}
              executer={executer}
            />
          ))
        )}
      </Carte>
    </div>
  );
}

import { urlInstanceCockpit } from '../api.js';
import { etapesSuivi, euros, statutLisible, tonStatut, vignette } from '../domaine.js';
import { Badge, Carte, EtatVide } from './ui.jsx';

function Suivi({ commande, tacheActive }) {
  const etapes = etapesSuivi(commande, tacheActive);
  return (
    <ol className="suivi">
      {etapes.map((etape) => (
        <li key={etape.cle} className={`suivi__etape suivi__etape--${etape.etat}`}>
          <span className="suivi__point" aria-hidden="true" />
          <span className="suivi__libelle">{etape.libelle}</span>
        </li>
      ))}
    </ol>
  );
}

export default function Commandes({ commandes, taches, selection, surSelection }) {
  if (commandes.length === 0) {
    return (
      <EtatVide icone="🧾" titre="Aucune commande pour le moment">
        Passez une commande depuis la boutique : une instance de processus démarre aussitôt.
      </EtatVide>
    );
  }

  const tacheDe = (commandeId) => taches.find((tache) => tache.commandeId === commandeId);
  const recentes = [...commandes].reverse();

  return (
    <div className="commandes">
      {recentes.map((commande) => {
        const ouverte = selection === commande.id;
        const tacheActive = tacheDe(commande.id);

        return (
          <Carte key={commande.id} className={`commande ${ouverte ? 'commande--ouverte' : ''}`}>
            <button
              className="commande__entete"
              onClick={() => surSelection(ouverte ? null : commande.id)}
              aria-expanded={ouverte}
            >
              <span className="commande__vignette">{vignette(commande.produit)}</span>

              <span className="commande__identite">
                <strong>{commande.id}</strong>
                <span className="commande__sous-titre">
                  {commande.quantite} × {commande.produit} — {commande.client}
                  {commande.typeClient === 'VIP' && <Badge ton="vip">VIP</Badge>}
                </span>
              </span>

              <span className="commande__montant">
                {commande.montant > 0 ? euros(commande.montant) : '—'}
              </span>

              <Badge ton={tonStatut(commande.statut)}>{statutLisible(commande.statut)}</Badge>

              <span className="commande__chevron" aria-hidden="true">
                {ouverte ? '▾' : '▸'}
              </span>
            </button>

            {ouverte && (
              <div className="commande__detail">
                <Suivi commande={commande} tacheActive={tacheActive} />

                <dl className="fiche">
                  <div>
                    <dt>Email</dt>
                    <dd>{commande.email}</dd>
                  </div>
                  <div>
                    <dt>Numéro de suivi</dt>
                    <dd>{commande.numeroSuivi ?? 'Pas encore expédiée'}</dd>
                  </div>
                  <div>
                    <dt>Tâche en attente</dt>
                    <dd>{tacheActive ? tacheActive.nom : 'Aucune'}</dd>
                  </div>
                  <div>
                    <dt>Instance Camunda</dt>
                    <dd>
                      {commande.processInstanceId ? (
                        <a
                          href={urlInstanceCockpit(commande.processInstanceId)}
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
              </div>
            )}
          </Carte>
        );
      })}
    </div>
  );
}

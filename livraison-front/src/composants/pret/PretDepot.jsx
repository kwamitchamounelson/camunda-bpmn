import { useState } from 'react';
import { api } from '../../api.js';
import { SCENARIOS_PRET } from '../../domainePret.js';
import { Bouton, Carte, Champ } from '../ui.jsx';

const CHAMPS_NUMERIQUES = [
  { cle: 'montantDemande', label: 'Montant emprunté (€)' },
  { cle: 'dureeMois', label: 'Durée (mois)' },
  { cle: 'revenusMensuels', label: 'Revenus mensuels nets (€)', indice: '0 = pièce manquante' },
  { cle: 'apport', label: 'Apport personnel (€)' },
  { cle: 'valeurBien', label: 'Prix du bien (€)', indice: '0 = pièce manquante' },
];

export default function PretDepot({ executer, surDemandeDeposee }) {
  const [scenario, setScenario] = useState(SCENARIOS_PRET[0].cle);
  const [demande, setDemande] = useState(SCENARIOS_PRET[0].demande);
  const [enCours, setEnCours] = useState(false);

  function choisir(element) {
    setScenario(element.cle);
    setDemande(element.demande);
  }

  const maj = (cle, valeur) => {
    setScenario(null);
    setDemande((actuelle) => ({ ...actuelle, [cle]: valeur }));
  };

  async function deposer() {
    setEnCours(true);
    const deposee = await executer(
      () => api.deposerPret(demande),
      (creee) => `Demande ${creee.id} déposée — le processus de prêt est démarré`,
    );
    setEnCours(false);
    if (deposee) surDemandeDeposee(deposee);
  }

  return (
    <>
      <Carte titre="Scénarios de test">
        <p className="texte-doux">
          Chaque scénario pré-remplit le dossier pour emprunter une branche différente du BPMN.
        </p>
        <div className="scenarios">
          {SCENARIOS_PRET.map((element) => (
            <button
              key={element.cle}
              className={`scenario ${scenario === element.cle ? 'scenario--actif' : ''}`}
              onClick={() => choisir(element)}
            >
              <span className="scenario__icone" aria-hidden="true">
                {element.icone}
              </span>
              <strong>{element.titre}</strong>
              <span className="scenario__effet">{element.effet}</span>
            </button>
          ))}
        </div>
      </Carte>

      <Carte titre="Demande de prêt">
        <div className="grille-formulaire">
          <Champ label="Emprunteur" indice="« Paul Fiche » et « Lea Moyenne » ont un profil simulé">
            <input value={demande.emprunteur} onChange={(e) => maj('emprunteur', e.target.value)} />
          </Champ>
          <Champ label="Email">
            <input
              type="email"
              value={demande.email}
              placeholder="optionnel"
              onChange={(e) => maj('email', e.target.value)}
            />
          </Champ>
          {CHAMPS_NUMERIQUES.map((champ) => (
            <Champ key={champ.cle} label={champ.label} indice={champ.indice}>
              <input
                type="number"
                min="0"
                value={demande[champ.cle]}
                onChange={(e) => maj(champ.cle, Number(e.target.value))}
              />
            </Champ>
          ))}
        </div>

        <div className="formulaire__actions">
          <Bouton variante="principal" disabled={enCours} onClick={deposer}>
            {enCours ? 'Envoi…' : 'Déposer la demande'}
          </Bouton>
        </div>
      </Carte>
    </>
  );
}

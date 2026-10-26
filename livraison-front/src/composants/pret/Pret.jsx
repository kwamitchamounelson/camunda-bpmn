import { useState } from 'react';
import PretDepot from './PretDepot.jsx';
import PretDossiers from './PretDossiers.jsx';
import PretTaches from './PretTaches.jsx';

const SECTIONS = [
  { cle: 'depot', libelle: 'Déposer une demande' },
  { cle: 'dossiers', libelle: 'Dossiers' },
  { cle: 'taches', libelle: 'Back-office prêt' },
];

/**
 * Onglet du second processus heberge par le moteur : ProcessusPretHypothecaire.
 * Meme principe que la livraison : chaque clic est un appel REST visible
 * dans le journal, le moteur fait le reste.
 */
export default function Pret({ prets, taches, executer }) {
  const [section, setSection] = useState('depot');
  const [selection, setSelection] = useState(null);

  return (
    <>
      <nav className="sous-navigation" aria-label="Sections du prêt immobilier">
        {SECTIONS.map((element) => (
          <button
            key={element.cle}
            className={`sous-navigation__bouton ${section === element.cle ? 'sous-navigation__bouton--actif' : ''}`}
            onClick={() => setSection(element.cle)}
          >
            {element.libelle}
            {element.cle === 'taches' && taches.length > 0 && (
              <span className="onglet__pastille">{taches.length}</span>
            )}
            {element.cle === 'dossiers' && prets.length > 0 && (
              <span className="sous-navigation__compte">{prets.length}</span>
            )}
          </button>
        ))}
      </nav>

      {section === 'depot' && (
        <PretDepot
          executer={executer}
          surDemandeDeposee={(demande) => {
            setSelection(demande.id);
            setSection('dossiers');
          }}
        />
      )}

      {section === 'dossiers' && (
        <PretDossiers
          prets={prets}
          taches={taches}
          selection={selection}
          surSelection={setSelection}
          executer={executer}
        />
      )}

      {section === 'taches' && <PretTaches taches={taches} prets={prets} executer={executer} />}
    </>
  );
}

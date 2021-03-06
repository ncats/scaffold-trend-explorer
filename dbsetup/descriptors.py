from rdkit.Chem import rdBase, RDConfig
from rdkit import Chem
from rdkit.Chem.Descriptors import qed
from rdkit.Chem.rdMolDescriptors import CalcFractionCSP3
import sys
import psycopg2
import psycopg2.extras
import sascorer


molsql = "select molregno, canonical_smiles from compound_structures"
insertsql = "update ste_descriptors set XXX = data.val from (VALUES %s) as data (id,val)  where molregno = data.id"

def SAScore(m):
    return sascorer.calculateScore(m)
    
def compute_descriptor(con, label, func):
    cursor = con.cursor('cursor_unique_name', cursor_factory=psycopg2.extras.DictCursor)
    cursor.execute(molsql)

    isql = insertsql.replace('XXX', label)
    icursor = con.cursor() ## for inserts
    batch_size = 200
    batch = []
    i = 0
    nmol = 0
    for molregno,smi in cursor:
        nmol += 1
        if smi is None:
            next

        try:
            m = Chem.MolFromSmiles(smi)
            batch.append((molregno, func(m)))
            i += 1
            if i == batch_size:
                psycopg2.extras.execute_values(icursor, isql, batch,
                                                   template=None, page_size=200)
                batch = []
                i = 0
            if nmol % 50 == 0:
                sys.stdout.write("\rProcessed %d" % (nmol))
                sys.stdout.flush()
        except:
            sys.stdout.write("\nSkipping %d\n" % (molregno))
            sys.stdout.flush()

if __name__ == '__main__':
    con = psycopg2.connect("dbname='chembl_23' user='chembl_23' port=5433 host='ifxdev.ncats.nih.gov' password='chembl_23'")
    #compute_descriptor(con, "qed", qed)
    #con.commit()    
    #compute_descriptor(con, "fsp3", CalcFractionCSP3)
    #con.commit()
    compute_descriptor(con, "sa", SAScore)
    con.commit()
    con.close()
    print

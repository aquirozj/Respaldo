/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.klan.proyecto.controlador;

import com.klan.proyecto.controlador.exceptions.NonexistentEntityException;
import com.klan.proyecto.controlador.exceptions.PreexistingEntityException;
import com.klan.proyecto.modelo.Evaluacion;
import com.klan.proyecto.modelo.EvaluacionPK;
import java.io.Serializable;
import javax.persistence.Query;
import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import com.klan.proyecto.modelo.Puesto;
import com.klan.proyecto.modelo.Usuario;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * Controlador JPA de la Clase Evaluación. Utilizada para realizar consultas y modificaciones
 *  a la tabla Evaluación de la BD.
 * @author patlani
 */
public class EvaluacionC implements Serializable {

    /**
     * Constructor a partir de una entidad de conexión a una base de datos.
     * @param emf Es la entidad que con la que se conecta a la base de datos.
     */
    public EvaluacionC(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    /**
     * Método que inserta en la BD una evaluación con sus atributos definidos.
     * Verificando que la PK este bien definida y sus listas de relaciones queden actualizadas.
     * @param evaluacion Es la evaluacipon a insertar.
     * @throws PreexistingEntityException Indica que ya exisistía una evaluación con la misma PK.
     * @throws Exception  Cualquier otro error durante la transacción.
     */
    public void create(Evaluacion evaluacion) throws PreexistingEntityException, Exception {
        // Se verifica que la evaluación tenga definida una llave primaria para insertar.
        if (evaluacion.getEvaluacionPK() == null) {
            throw new NullPointerException("Se debe definir una llave no nula para insertar la evaluación.");
        } // Se guarda la llave primaria como id para usarla más adelante.
        EvaluacionPK id = evaluacion.getEvaluacionPK();
        EntityManager em = null;
        try { // Se comienza la transacción de inserción en la BD.
            em = getEntityManager();
            em.getTransaction().begin();
            // Se busca la referencia al puesto y usuario definidos en la llave primaria.
            Puesto puesto = em.getReference(Puesto.class, id.getNombrePuesto());
            Usuario usuario = em.getReference(Usuario.class, id.getNombreUsuario());
            // Se asegura que la llave primaria sea congruente con los atributos de la evaluación.
            evaluacion.setPuesto(puesto); evaluacion.setUsuario(usuario);
            // Se verifica que las referencias no sean nulas.
            if (evaluacion.getPuesto() == null || evaluacion.getUsuario() == null) {
                // Se cierra la conexión antes de cancelar la inserción.
                em.close(); throw new NullPointerException("La llave de la evaluación contiene referencias nulas.");
            } // Se asigna el siguiente id de la numeración seriada según el número de entidades en la tabla.
            evaluacion.setIdEvaluacion(getEvaluacionCount() + 1);
            // Se agrega la evaluación a la lista de evaluaciones relacionadas con el puesto.
            puesto.getEvaluacionList().add(evaluacion); em.merge(puesto);
            // Se agrega la evaluación a la lista de evaluaciones relacionadas con el usuario.
            usuario.getEvaluacionList().add(evaluacion); em.merge(usuario);
            em.persist(evaluacion); // Se inserta la evaluación en la BD.
            em.getTransaction().commit(); // Se confirma la transacción.
        } catch (Exception ex) {
            if (findEvaluacion(id) != null) {
                throw new PreexistingEntityException("La evaluación " + evaluacion + " ya existe en la BD.", ex);
            } throw ex;
        } finally {
            if (em != null) em.close();
        }
    }

    /**
     * Método que actualiza en la BD una evaluación con sus atributos definidos.
     * Verificando que la PK este bien definida y sus listas de relaciones queden actualizadas.
     * @param evaluacion Es la evaluacipon que se actualiza.
     * @throws NonexistentEntityException Indica que la evaluación no existe en la BD.
     * @throws Exception Cualquier error que ocurra durante la transacción.
     */
    public void edit(Evaluacion evaluacion) throws NonexistentEntityException, Exception {
        // Se verifica que la evaluación tenga definida una llave primaria para actualizar.
        if (evaluacion.getEvaluacionPK() == null) {
            throw new NullPointerException("Se debe definir una llave no nula para insertar la evaluación.");
        } // Se guarda la llave primaria como id para usarla más adelante.
        EvaluacionPK id = evaluacion.getEvaluacionPK();
        EntityManager em = null;
        try { // Se verifica que la evaluación con la PK deifinida, exista en la BD.
            Evaluacion original = findEvaluacion(id);
            if (original == null) throw new NonexistentEntityException("La evaluación con id " + id + "no existe.");
            // Se comienza la transacción de actualización en la BD.
            em = getEntityManager();
            em.getTransaction().begin();
            // Se busca la referencia al puesto y usuario definidos en la llave primaria.
            Puesto puesto = em.getReference(Puesto.class, id.getNombrePuesto());
            Usuario usuario = em.getReference(Usuario.class, id.getNombreUsuario());
            // Se asegura que la llave primaria sea congruente con los atributos de la evaluación.
            evaluacion.setPuesto(puesto); evaluacion.setUsuario(usuario);
            // Se verifica que las referencias no sean nulas.
            if (evaluacion.getPuesto() == null || evaluacion.getUsuario() == null) {
                // Se cierra la conexión antes de cancelar la inserción.
                em.close(); throw new NullPointerException("La llave de la evaluación contiene referencias nulas.");
            } // Se actualiza la evaluación de la lista de evaluaciones relacionadas con el puesto.
            puesto.getEvaluacionList().remove(original);
            puesto.getEvaluacionList().add(evaluacion);
            em.merge(puesto); // Se actualiza el puesto en la BD.
            // Se actualiza la evaluación de la lista de evaluaciones relacionadas con el usuario.
            usuario.getEvaluacionList().remove(original);
            usuario.getEvaluacionList().add(evaluacion);
            em.merge(usuario); // Se actualiza el usuario en la BD.
            em.merge(evaluacion); // Se actualiza la evaluación en la BD.
            em.getTransaction().commit(); // Se confirma la transacción.
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    /**
     * Método que elimina de la BD una evaluación con sus atributos definidos.
     * Verificando que la PK este bien definida y sus listas de relaciones queden actualizadas.
     * @param id Es la llave primaria de la evaluación.
     * @throws NonexistentEntityException Indica que la evaluación no se elimina porque existe en la BD.
     */
    public void destroy(EvaluacionPK id) throws NonexistentEntityException {
        EntityManager em = null;
        try { // Se comienza la transacción en la BD.
            em = getEntityManager();
            em.getTransaction().begin();
            Evaluacion evaluacion;
            try { // Se busca la evaluación con la PK definida en la BD.
                evaluacion = em.getReference(Evaluacion.class, id);
                evaluacion.getEvaluacionPK();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("La evaluación con id " + id + " no existe en la BD.", enfe);
            } // Se remueve la relación del puesto con la evaluación.
            Puesto puesto = evaluacion.getPuesto();
            if (puesto != null) {
                puesto.getEvaluacionList().remove(evaluacion);
                em.merge(puesto);
            } // Se remueve la relación del usuario con la evaluación.
            Usuario usuario = evaluacion.getUsuario();
            if (usuario != null) {
                usuario.getEvaluacionList().remove(evaluacion);
                em.merge(usuario);
            } // Se remueve la evaluación de la BD.
            em.remove(evaluacion);
            // Se confirma la transacción.
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    /**
     * Método que obtiene la lista de entidades en la tabla Evaluación de la BD.
     * @return Devuelve una lista con las evaluaciónes insertadas en la BD.
     */
    public List<Evaluacion> findEvaluacionEntities() {
        return findEvaluacionEntities(true, -1, -1);
    }

    public List<Evaluacion> findEvaluacionEntities(int maxResults, int firstResult) {
        return findEvaluacionEntities(false, maxResults, firstResult);
    }

    private List<Evaluacion> findEvaluacionEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Evaluacion.class));
            Query q = em.createQuery(cq);
            if (!all) {
                q.setMaxResults(maxResults);
                q.setFirstResult(firstResult);
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Método que realiza la búsqueda de una evaluación por su llave primaria.
     * @param id Es la llave primaria de la evaluación que se busca, definida con
     * un nombre de puesto y un nombre de usuario.
     * @return Devuelve la entidad encontrada, o NULL si no se encuentra.
     */
    public Evaluacion findEvaluacion(EvaluacionPK id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Evaluacion.class, id);
        } finally {
            em.close();
        }
    }

    /**
     * Devuelve la cantidad de entidades en la tabla Evaluación de la BD.
     * @return Devuelve un entero con la cantidad de entidades en la tabla Evaluación de la BD.
     */
    public int getEvaluacionCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Evaluacion> rt = cq.from(Evaluacion.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
}

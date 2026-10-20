package ar.edu.utn.frc.bda.depositoclick.compras.services;

import ar.edu.utn.frc.bda.depositoclick.compras.dtos.ArticuloProveedorResponse;
import ar.edu.utn.frc.bda.depositoclick.compras.dtos.OrdenResponse;
import ar.edu.utn.frc.bda.depositoclick.compras.dtos.ProveedorDTO;
import ar.edu.utn.frc.bda.depositoclick.compras.models.ArticuloProveedor;
import ar.edu.utn.frc.bda.depositoclick.compras.models.DetalleOrden;
import ar.edu.utn.frc.bda.depositoclick.compras.models.OrdenCompra;
import ar.edu.utn.frc.bda.depositoclick.compras.models.Proveedor;
import ar.edu.utn.frc.bda.depositoclick.compras.models.Recepcion;

public final class ComprasMapper {

    private ComprasMapper() {
    }

    public static ProveedorDTO toDTO(Proveedor p) {
        return ProveedorDTO.builder()
                .id(p.getId())
                .cuit(p.getCuit())
                .razonSocial(p.getRazonSocial())
                .email(p.getEmail())
                .telefono(p.getTelefono())
                .leadTimeDiasHabiles(p.getLeadTimeDiasHabiles())
                .activo(p.isActivo())
                .build();
    }

    public static ArticuloProveedorResponse toResponse(ArticuloProveedor ap) {
        return ArticuloProveedorResponse.builder()
                .id(ap.getId())
                .proveedorId(ap.getProveedor().getId())
                .proveedorRazonSocial(ap.getProveedor().getRazonSocial())
                .articuloId(ap.getArticuloId())
                .sku(ap.getSku())
                .nombreArticulo(ap.getNombreArticulo())
                .precioUnitario(ap.getPrecioUnitario())
                .preferido(ap.isPreferido())
                .build();
    }

    public static OrdenResponse toResponse(OrdenCompra o) {
        return OrdenResponse.builder()
                .id(o.getId())
                .estado(o.getEstado())
                .proveedorId(o.getProveedor().getId())
                .proveedorRazonSocial(o.getProveedor().getRazonSocial())
                .leadTimeDiasHabiles(o.getProveedor().getLeadTimeDiasHabiles())
                .depositoDestinoId(o.getDepositoDestinoId())
                .depositoCodigo(o.getDepositoCodigo())
                .paisDeposito(o.getPaisDeposito())
                .fechaCreacion(o.getFechaCreacion())
                .fechaEmision(o.getFechaEmision())
                .fechaEntregaSolicitada(o.getFechaEntregaSolicitada())
                .fechaEntregaEstimada(o.getFechaEntregaEstimada())
                .fuenteCalendario(o.getFuenteCalendario())
                .calendarioConfiable(o.getCalendarioConfiable())
                .fechaCancelacion(o.getFechaCancelacion())
                .motivoCancelacion(o.getMotivoCancelacion())
                .observacion(o.getObservacion())
                .creadoPor(o.getCreadoPor())
                .total(o.getTotal())
                .detalles(o.getDetalles().stream().map(ComprasMapper::toDetalle).toList())
                .recepciones(o.getRecepciones().stream().map(ComprasMapper::toRecepcion).toList())
                .build();
    }

    private static OrdenResponse.Detalle toDetalle(DetalleOrden d) {
        return OrdenResponse.Detalle.builder()
                .articuloId(d.getArticuloId())
                .sku(d.getSku())
                .nombreArticulo(d.getNombreArticulo())
                .unidadMedida(d.getUnidadMedida())
                .cantidadPedida(d.getCantidadPedida())
                .cantidadRecibida(d.getCantidadRecibida())
                .pendiente(d.getPendiente())
                .precioUnitario(d.getPrecioUnitario())
                .subtotal(d.getSubtotal())
                .build();
    }

    private static OrdenResponse.RecepcionInfo toRecepcion(Recepcion r) {
        return OrdenResponse.RecepcionInfo.builder()
                .numero(r.getNumero())
                .fecha(r.getFecha())
                .usuario(r.getUsuario())
                .referenciaInventario(r.getReferenciaInventario())
                .observacion(r.getObservacion())
                .items(r.getItems().stream()
                        .map(i -> new OrdenResponse.ItemRecibido(i.getArticuloId(), i.getCantidad(), i.getLote(), i.getFechaVencimiento()))
                        .toList())
                .build();
    }
}
